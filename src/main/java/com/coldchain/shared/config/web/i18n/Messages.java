package com.coldchain.shared.config.web.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * The one way to turn a message key into a sentence. The English text the code carries is the
 * default, so a key nobody translated yet answers in English rather than failing the request.
 */
@Component
public class Messages {

    private final MessageSource messages;

    public Messages(MessageSource messages) {
        this.messages = messages;
    }

    public String of(String messageKey, String english) {
        return messages.getMessage(messageKey, null, english, locale());
    }

    private Locale locale() {
        Locale asked = LocaleContextHolder.getLocale();
        return MessageConfig.SUPPORTED_LOCALES.stream()
                .filter(supported -> supported.getLanguage().equals(asked.getLanguage()))
                .findFirst()
                .orElse(MessageConfig.DEFAULT_LOCALE);
    }
}
