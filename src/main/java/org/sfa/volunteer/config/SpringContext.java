package org.sfa.volunteer.config;

import lombok.Getter;
import org.sfa.volunteer.VolunteerApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Getter
public class SpringContext {
    private static ApplicationContext context;

    public static synchronized ApplicationContext getContext() {
        if (context == null) {
            context = new AnnotationConfigApplicationContext(VolunteerApplication.class);
        }
        return context;
    }
}