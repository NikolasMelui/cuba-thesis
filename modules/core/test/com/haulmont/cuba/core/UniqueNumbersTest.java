/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.haulmont.cuba.core.app.UniqueNumbersAPI;
import com.haulmont.cuba.core.global.AppBeans;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author krivopustov
 * @version $Id$
 */
public class UniqueNumbersTest extends CubaTestCase {

    String[] seqNames = {"t1", "t2", "t3", "t4"};
    AtomicInteger finishedThreads = new AtomicInteger();
    AtomicInteger exceptionCnt = new AtomicInteger();

    public void test() {
        UniqueNumbersAPI mBean = AppBeans.get(UniqueNumbersAPI.NAME);
        long n = mBean.getNextNumber("test1");
        assertTrue(n >= 0);
    }

    public void testSequenceDeletion() throws Exception {
        UniqueNumbersAPI uniqueNumbersAPI = AppBeans.get(UniqueNumbersAPI.NAME);

        uniqueNumbersAPI.getCurrentNumber("s1");
        uniqueNumbersAPI.deleteSequence("s1");
        uniqueNumbersAPI.getCurrentNumber("s1");
    }

    public void testConcurrentModification() throws Exception {
        int threadCnt = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCnt,
                new ThreadFactoryBuilder().setNameFormat("T%d").build());

        final Action[] actions = {new SleepAction(), new GetNumberAction(),
                new SetNumberAction(), new DeleteSequenceAction()};

        for (int i = 0; i < threadCnt; i++) {
            final int finalI = i;
            executorService.submit(new Runnable() {

                int runnableNo = finalI;

                @Override
                public void run() {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int i = 0; i < 10; i++) {
                        int action = random.nextInt(0, 4);
                        System.out.println("Runnable " + runnableNo + " iteration " + i + " action " + actions[action]);
                        try {
                            int seqN = actions[action].perform(runnableNo);
                            actions[action].success(runnableNo, seqN);
                        } catch (Exception e) {
                            if (e instanceof IllegalStateException && StringUtils.contains(e.getMessage(), "Attempt to delete")) {
                                System.err.println(e.getMessage());
                                continue;
                            }
                            System.err.println(e.getMessage());
                            exceptionCnt.incrementAndGet();
                        }
                    }
                    finishedThreads.incrementAndGet();
                }
            });
        }

        while (finishedThreads.get() < threadCnt) {
            System.out.println("Waiting...");
            TimeUnit.MILLISECONDS.sleep(200);
        }

        assertEquals(exceptionCnt.get(), 0);
    }

    protected abstract class Action {
        abstract int perform(int threadNumber);

        abstract void success(int threadNumber, int seqN);
    }

    protected class SleepAction extends Action {
        @Override
        public int perform(int threadNumber) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        void success(int threadNumber, int seqN) {
            System.out.println("Runnable " + threadNumber + " " + Thread.currentThread().getName() + " slept 10 ms");
        }
    }

    protected class GetNumberAction extends Action {
        @Override
        public int perform(int threadNumber) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int nextInt = random.nextInt(0, 3);
            UniqueNumbersAPI api = AppBeans.get(UniqueNumbersAPI.NAME);
            long nextNumber = api.getNextNumber(seqNames[nextInt]);
            return nextInt;
        }

        @Override
        void success(int threadNumber, int seqN) {
            System.out.println("Runnable " + threadNumber + " " + Thread.currentThread().getName() +
                    " got number from seq t" + seqN);
        }
    }

    protected class SetNumberAction extends Action {

        @Override
        public int perform(int threadNumber) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int nextInt = random.nextInt(0, 3);
            UniqueNumbersAPI api = AppBeans.get(UniqueNumbersAPI.NAME);
            int nextNum = random.nextInt(0, 10000);
            api.setCurrentNumber(seqNames[nextInt], nextNum);
            return nextInt;
        }

        @Override
        void success(int threadNumber, int seqN) {
            System.out.println("Runnable " + threadNumber + " " + Thread.currentThread().getName() +
                    " set number to seq t" + seqN);
        }
    }

    protected class DeleteSequenceAction extends Action {

        @Override
        public int perform(int threadNumber) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int nextInt = random.nextInt(0, 3);
            UniqueNumbersAPI api = AppBeans.get(UniqueNumbersAPI.NAME);
            api.deleteSequence(seqNames[nextInt]);
            return nextInt;
        }

        @Override
        void success(int threadNumber, int seqN) {
            System.out.println("Runnable " + threadNumber + " " +
                    Thread.currentThread().getName() + " dropped seq t" + seqN);
        }
    }
}