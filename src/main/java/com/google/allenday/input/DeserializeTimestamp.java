package com.google.allenday.input;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;


import java.io.IOException;

public class DeserializeTimestamp extends JsonDeserializer<Long> {

    private static final DateTimeFormatter dateTimeFormatter =
            new DateTimeFormatterBuilder()
                    .append(ISODateTimeFormat.date())
                    .appendLiteral(' ')
                    .append(ISODateTimeFormat.hourMinuteSecond())
                    .appendLiteral(' ')
                    .appendTimeZoneId()
                    .toFormatter();

    @Override
    public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Instant ts = parseTimestamp(jsonParser.getText());

        return ts.getMillis();
    }

    /**
     * Parses time string
     *
     * @param ts string in following format "2018-05-26 00:35:42 UTC"
     * @return Instant
     */
    private Instant parseTimestamp(String ts) {
        return Instant.parse(ts, dateTimeFormatter);
    }
}
