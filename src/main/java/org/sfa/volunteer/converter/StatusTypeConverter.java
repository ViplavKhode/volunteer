package org.sfa.volunteer.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.sfa.volunteer.model.StatusType;

@Converter(autoApply = false)
public class StatusTypeConverter implements AttributeConverter<StatusType, String> {

    @Override
    public String convertToDatabaseColumn(StatusType attribute) {
        if (attribute == null) return null;
        // Store lowercase to match existing DB values: "read"/"unread"
        return attribute.name().toLowerCase();
    }

    @Override
    public StatusType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Reuse your enum logic (case-insensitive)
        return StatusType.fromValue(dbData);
    }
}
