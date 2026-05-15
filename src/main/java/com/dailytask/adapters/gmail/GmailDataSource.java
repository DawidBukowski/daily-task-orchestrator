package com.dailytask.adapters.gmail;

import com.dailytask.core.domain.RawTask;
import com.dailytask.core.ports.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GmailDataSource implements DataSource {
    private static final Logger logger = LoggerFactory.getLogger(GmailDataSource.class);

    @Override
    public List<RawTask> fetch() {
        logger.info("Fetching raw tasks from Gmail...");
        // TODO: Implement actual Gmail API fetching logic
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "Gmail API Source";
    }
}