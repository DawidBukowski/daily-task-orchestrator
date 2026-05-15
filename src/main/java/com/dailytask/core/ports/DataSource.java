package com.dailytask.core.ports;

import com.dailytask.core.domain.RawTask;
import java.util.List;

/**
 * Contract for fetching raw task data from external platforms.
 */
public interface DataSource {
    List<RawTask> fetch();
    String getName();
}