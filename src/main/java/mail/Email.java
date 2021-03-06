package mail;

import lombok.Builder;
import lombok.extern.java.Log;

import javax.mail.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log
public class Email {

    private Store store;
    private Folder emailFolder;

    @Builder
    public Email(String email, String password) {
        this.store = createStore(email, password);
    }

    public List<Message> getAllMessages() {

        try {
            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);
            return Arrays.stream(emailFolder.getMessages()).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void deleteAllMessages() {
        try {
            getAllMessages().forEach(message -> {
                try {
                    message.setFlag(Flags.Flag.DELETED, true);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
            emailFolder.close(true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public boolean isMessageReceived() {

        LocalDateTime start = LocalDateTime.now();
        Stream.generate(this::getAllMessages)
                .peek(messages -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .takeWhile(list -> list.size() == 0 )
                .takeWhile(webElement -> Duration.between(start, LocalDateTime.now()).toSeconds() < 180)
                .forEach(webElement -> {});

        return getAllMessages().size() > 0;
    }

    public boolean isMessageReceived(String messageName) {

        LocalDateTime start = LocalDateTime.now();

        AtomicReference<Message> specificMessage = new AtomicReference<>();
        Stream.generate(this::getAllMessages)
                .peek(messages -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .takeWhile(list -> list.stream()
                        .filter(message -> {
                            try {
                                log.info("Email message: " + message.getSubject());
                                return message.getSubject().contains(messageName);
                            } catch (Exception e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        })
                        .peek(specificMessage::getAndSet)
                        .collect(Collectors.toList()).size() == 0)
                .takeWhile(webElement -> Duration.between(start, LocalDateTime.now()).toSeconds() < 180)
                .forEach(messages -> {});

        return specificMessage.get() != null;
    }




    private Store createStore(String email, String password) {
        Properties properties = new Properties();
        properties.put("mail.imap.user", "imap.ukr.net");
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.ssl.enable", "true");
        Session emailSession = Session.getDefaultInstance(properties);

        try {
            store = emailSession.getStore("imap");
            store.connect("imap.ukr.net", email, password);
            return store;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println(Email.builder().email("").password("").build()
                .isMessageReceived("[OpenALPR] Activate Account"));
    }
}
