package com.coldchain.shared.config.web.i18n;

import com.coldchain.shared.infrastructure.log.LogMessages;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Where the message keys that travel in every response are turned into sentences.
 */
@Configuration
public class MessageConfig {

    static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.ENGLISH, Locale.forLanguageTag("es"), Locale.forLanguageTag("pt"));

    private static final Logger LOG = LoggerFactory.getLogger(MessageConfig.class);

    private static final List<String> BUNDLE_PATTERNS = List.of(
            "classpath*:i18n/*.properties",
            "classpath*:modules/*/i18n/*.properties");

    private static final String PROPERTIES = ".properties";

    /**
     * The catalogue is discovered rather than listed, so a module that brings its own bundle is
     * read without anybody remembering to register it.
     * <p>
     * {@code useCodeAsDefaultMessage} stays false on purpose: with it on, the source answers every
     * key, the missing ones included, and a key nobody translated reaches the client looking like a
     * sentence. Every call site passes the English text as its default, so a missing key degrades
     * to English instead of leaking {@code error.catalog.profile_not_found} to a human.
     */
    @Bean
    MessageSource messageSource() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        String[] basenames = discoverBasenames();
        messages.setBasenames(basenames);
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        messages.setUseCodeAsDefaultMessage(false);
        LOG.info(LogMessages.I18N_BUNDLES_DISCOVERED, basenames.length);
        return messages;
    }

    /**
     * Bean validation resolves its own templates against its own bundle unless it is handed this
     * one, which is why a violation used to answer in English inside a Spanish response.
     */
    @Bean
    LocalValidatorFactoryBean defaultValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        return resolver;
    }

    private String[] discoverBasenames() {
        Set<String> basenames = new TreeSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : BUNDLE_PATTERNS) {
            try {
                for (Resource bundle : resolver.getResources(pattern)) {
                    basenameOf(bundle).ifPresent(basenames::add);
                }
            } catch (IOException unreadable) {
                LOG.warn(LogMessages.I18N_SCAN_FAILED, pattern, unreadable);
            }
        }
        return basenames.toArray(String[]::new);
    }

    private Optional<String> basenameOf(Resource bundle) {
        try {
            String uri = bundle.getURI().toString();
            int start = uri.lastIndexOf("/i18n/");
            if (start < 0 || !uri.endsWith(PROPERTIES)) {
                return Optional.empty();
            }
            String path = uri.substring(0, uri.length() - PROPERTIES.length());
            int folder = uri.lastIndexOf('/', start - 1);
            String relative = path.substring(folder + 1);
            return Optional.of(withoutLanguage(relative));
        } catch (IOException unreadable) {
            LOG.warn(LogMessages.I18N_SCAN_FAILED, bundle.getDescription(), unreadable);
            return Optional.empty();
        }
    }

    private String withoutLanguage(String basename) {
        int separator = basename.lastIndexOf('_');
        if (separator < 0) {
            return basename;
        }
        String suffix = basename.substring(separator + 1);
        return SUPPORTED_LOCALES.stream().anyMatch(locale -> locale.getLanguage().equals(suffix))
                ? basename.substring(0, separator)
                : basename;
    }
}
