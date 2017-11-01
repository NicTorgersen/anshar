package no.rutebanken.anshar.subscription;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.time.Duration;

@Converter
public class DurationConverter implements AttributeConverter<Duration, String> {
    @Override
    public String convertToDatabaseColumn(Duration duration) {
        if (duration == null) return null;
        return duration.toString();
    }

    @Override
    public Duration convertToEntityAttribute(String s) {
        if (s == null) return null;
        return Duration.parse(s);
    }
}
