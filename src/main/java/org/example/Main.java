package org.example;


import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.messages.MessageAttachment;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;

import javax.print.*;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Random;

public class Main {
    static String accessToken = "4da688544537d33cd6f120a4cef3e678b84074bae59038ed9729db382f697395c418802676078551790e7";
    static String director = "C:\\Users\\duduc\\OneDrive\\Рабочий стол";
    static Integer groupid = 213452188;
    public static void getPrinterNames() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services.length != 0 || services != null) {
            int i = 0;
            for (PrintService service : services) {
                System.out.println(i + " " + service.getName());
                i++;
            }
        }
    }

    public static void getPhotoAndPrint(MessageAttachment attachment) throws IOException {
        System.out.println(attachment.getPhoto().getSizes().get(attachment.getPhoto().getSizes().size() - 1).getUrl());
        URL url = new URL(attachment.getPhoto().getSizes().get(attachment.getPhoto().getSizes().size() - 1).getUrl().toString());
        Files.copy(url.openStream(), Paths.get(director + attachment.getPhoto().getId() + ".jpeg"), StandardCopyOption.REPLACE_EXISTING);
        try {
            printJpeg(director+ attachment.getPhoto().getId() + ".jpeg");
        } catch (PrintException e) {
            throw new RuntimeException(e);
        }
    }

    public static void printJpeg(String file) throws FileNotFoundException, PrintException {
        File f = new File(file);
        FileInputStream inputStream = new FileInputStream(f);
        DocFlavor flavor = DocFlavor.INPUT_STREAM.JPEG;
        PrintService[] printService = PrintServiceLookup.lookupPrintServices(flavor, null);
        DocPrintJob job = printService[1].createPrintJob();
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        DocAttributeSet das = new HashDocAttributeSet();
        Doc doc = new SimpleDoc(inputStream, flavor, das);
        job.print(doc, pras);
    }

    public static void main(String[] args) throws FileNotFoundException, PrintException, ClientException, ApiException, InterruptedException {
        TransportClient transportClient = new HttpTransportClient();
        VkApiClient vk = new VkApiClient(transportClient);
        Random random = new Random();
        GroupActor a = new GroupActor(groupid, accessToken);
        System.out.println(vk.messages().getLongPollServer(a).execute().toString());
        Integer ts = vk.messages().getLongPollServer(a).execute().getTs();
        while (true) {
            MessagesGetLongPollHistoryQuery historyQuery = vk.messages().getLongPollHistory(a).ts(ts);
            List<Message> messages = historyQuery.execute().getMessages().getItems();
            messages.forEach(message -> {
                if (!message.getAttachments().isEmpty())
                    message.getAttachments().forEach(attachment -> {
                        try {
                            switch (attachment.getType()) {
                                case PHOTO -> getPhotoAndPrint(attachment);
                                default -> vk.messages().send(a).userId(message.getId())
                                        .randomId(random.nextInt(10000))
                                        .message("Пока не поддерживается")
                                        .execute();
                            }
                        } catch (IOException | ApiException | ClientException e) {

                        }
                    });
            });
            ts = vk.messages().getLongPollServer(a).execute().getTs();
            Thread.sleep(500);
        }
    }
}

