/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.eip.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.v2.ScatterGather;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An instance of this class is created to manage each aggregation group, and it holds
 * the aggregation properties and the messages collected during aggregation. This class also
 * times out itself after the timeout expires it
 */
public class Aggregate extends TimerTask {

    private static final Log log = LogFactory.getLog(Aggregate.class);

    private long timeoutMillis = 0;
    /** The time in millis at which this aggregation should be considered as expired */
    private long expiryTimeMillis = 0;
    /** The minimum number of messages to be collected to consider this aggregation as complete */
    private int minCount = -1;
    /** The maximum number of messages that should be collected by this aggregation */
    private int maxCount = -1;
    private String correlation = null;
    /** The AggregateMediator that should be invoked on completion of the aggregation */
    private AggregateMediator aggregateMediator = null;
    private ScatterGather scatterGatherMediator = null;
    private List<MessageContext> messages = new ArrayList<MessageContext>();
    private ReentrantLock lock = new ReentrantLock();
    private boolean completed = false;
    private SynapseEnvironment synEnv = null;

    /**
     * Fault handler for the aggregate mediator
     */
    private FaultHandler faultHandler;

    /**
     * Save aggregation properties and timeout
     *  @param corelation representing the corelation name of the messages in the aggregate
     * @param timeoutMillis the timeout duration in milliseconds
     * @param min the minimum number of messages to be aggregated
     * @param max the maximum number of messages to be aggregated
     * @param mediator
     * @param faultHandler
     */
    public Aggregate(SynapseEnvironment synEnv, String corelation, long timeoutMillis, int min,
                     int max, AggregateMediator mediator, FaultHandler faultHandler) {

        this.synEnv = synEnv;
        this.correlation = corelation;
        if (timeoutMillis > 0) {
            expiryTimeMillis = System.currentTimeMillis() + timeoutMillis;
        }
        if (min > 0) {
            minCount = min;
        }
        if (max > 0) {
            maxCount = max;
        }
        this.faultHandler = faultHandler;
        this.aggregateMediator = mediator;
    }

    public Aggregate(SynapseEnvironment synEnv, String corelation, long timeoutMillis, int min,
                     int max, ScatterGather scatterGatherMediator, FaultHandler faultHandler) {

        this.synEnv = synEnv;
        this.correlation = corelation;
        if (timeoutMillis > 0) {
            expiryTimeMillis = System.currentTimeMillis() + timeoutMillis;
        }
        if (min > 0) {
            minCount = min;
        }
        if (max > 0) {
            maxCount = max;
        }
        this.faultHandler = faultHandler;
        this.scatterGatherMediator = scatterGatherMediator;
    }

    /**
     * Add a message to the interlan message list
     *
     * @param synCtx message to be added into this aggregation group
     * @return true if the message was added or false if not
     */
    public synchronized boolean addMessage(MessageContext synCtx) {
        if (maxCount <= 0 || (maxCount > 0 && messages.size() < maxCount)) {
            messages.add(synCtx);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Has this aggregation group completed?
     *
     * @param synLog the Synapse log to use
     *
     * @return boolean true if aggregation is complete
     */
    public synchronized boolean isComplete(SynapseLog synLog) {

        if (!completed) {

            // if any messages have been collected, check if the completion criteria is met
            if (!messages.isEmpty()) {

                // get total messages for this group, from the first message we have collected
                MessageContext mc = messages.get(0);
                Object prop;
                if (aggregateMediator != null) {
                    prop = mc.getProperty(EIPConstants.MESSAGE_SEQUENCE +
                            (aggregateMediator.getId() != null ? "." + aggregateMediator.getId() : ""));
                } else {
                    prop = mc.getProperty(EIPConstants.MESSAGE_SEQUENCE +
                            (scatterGatherMediator.getId() != null ? "." + scatterGatherMediator.getId() : ""));
                }

                if (prop != null && prop instanceof String) {
                    String[] msgSequence = prop.toString().split(
                            EIPConstants.MESSAGE_SEQUENCE_DELEMITER);
                    int total = Integer.parseInt(msgSequence[1]);

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug(messages.size() +
                                " messages of " + total + " collected in current aggregation");
                    }

                    if (messages.size() >= total) {
                        synLog.traceOrDebug("Aggregation complete");
                        return true;
                    }
                }
            } else {
                synLog.traceOrDebug("No messages collected in current aggregation");
            }

            // if the minimum number of messages has been reached, its complete
            if (minCount > 0 && messages.size() >= minCount) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug(
                            "Aggregation complete - the minimum : " + minCount
                                    + " messages has been reached");
                }
                return true;
            }

            if (maxCount > 0 && messages.size() >= maxCount) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug(
                            "Aggregation complete - the maximum : " + maxCount
                                    + " messages has been reached");
                }

                return true;
            }

            // else, has this aggregation reached its timeout?
            if (expiryTimeMillis > 0 && System.currentTimeMillis() >= expiryTimeMillis) {
                synLog.traceOrDebug("Aggregation complete - the aggregation has timed out");

                return true;
            }
        } else {
            synLog.traceOrDebug(
                    "Aggregation already completed - this message will not be processed in aggregation");
        }
        
        return false;
    }

    public MessageContext getLastMessage() {
        return messages.get(messages.size() - 1);
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getMinCount() {
        return minCount;
    }

    public void setMinCount(int minCount) {
        this.minCount = minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getCorrelation() {
        return correlation;
    }

    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }

    public synchronized List<MessageContext> getMessages() {
        return new ArrayList<MessageContext>(messages);
    }

    public void setMessages(List<MessageContext> messages) {
        this.messages = messages;
    }

    public long getExpiryTimeMillis() {
        return expiryTimeMillis;
    }

    public void setExpiryTimeMillis(long expiryTimeMillis) {
        this.expiryTimeMillis = expiryTimeMillis;
    }

    public void run() {
        while (true) {
            if (completed) {
                break;
            }
            if (getLock()) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Time : " + System.currentTimeMillis() + " and this aggregator " +
                                "expired at : " + expiryTimeMillis);
                    }
                    synEnv.getExecutorService().execute(new AggregateTimeout(this));
                    break;
                } finally {
                    releaseLock();
                }
            }
        }
    }

    /**
     * Clear references in Aggregate Timer Task
     *
     * This need to be called when aggregation is completed.
     * Task is not eligible for gc until it reach the execution time,
     * even though it is cancelled. So we need to remove references from task to other objects to
     * allow them to be garbage collected
     *
     */
    public void clear() {
        messages = null;
    }

    private class AggregateTimeout implements Runnable {
        private Aggregate aggregate = null;
        AggregateTimeout(Aggregate aggregate) {
            this.aggregate = aggregate;
        }

        public void run() {
            MessageContext messageContext = aggregate.getLastMessage();
            try {
                if (aggregateMediator != null) {
                    log.warn("Aggregate mediator timeout occurred.");
                    aggregateMediator.completeAggregate(aggregate);
                } else {
                    log.warn("Scatter Gather mediator timeout occurred.");
                    scatterGatherMediator.completeAggregate(aggregate);
                }
            } catch (Exception ex) {
                if (faultHandler != null && messageContext != null) {
                    faultHandler.handleFault(messageContext, ex);
                } else {
                    log.error("Synapse encountered an exception, No error handlers found or no messages were " +
                            "aggregated - [Message Dropped]\n" + ex.getMessage());
                }
            }
        }
    }

    public synchronized boolean getLock() {

        return lock.tryLock();
    }

    public synchronized void releaseLock() {

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
