package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.model.Recording;

import java.util.List;

/**
 * Created by ddcbryanl on 1/27/17.
 */
public class FileDBService implements DbService {
    @Override
    public List<Recording> queryRecording(Class responseObjClass, String id, long timestampLowerBound, long timestampUpperBound, int limit, boolean ascendingOrder) {
        return null;
    }

    @Override
    public Recording writeRecording(Recording obj) {
        return null;
    }
}
