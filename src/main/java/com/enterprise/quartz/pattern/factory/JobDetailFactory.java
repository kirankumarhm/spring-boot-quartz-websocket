package com.enterprise.quartz.pattern.factory;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import org.quartz.JobDetail;

/**
 * Factory interface for constructing Quartz JobDetail instances.
 */
public interface JobDetailFactory {

    /**
     * Constructs a JobDetail domain instance from the request specification.
     *
     * @param request the create job request payload
     * @return constructed JobDetail
     */
    JobDetail createJobDetail(CreateJobRequest request);
}
