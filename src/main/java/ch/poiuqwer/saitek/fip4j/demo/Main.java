package ch.poiuqwer.saitek.fip4j.demo;

import ch.poiuqwer.saitek.fip4j.*;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.poiuqwer.saitek.fip4j.DeviceState.*;

/**
 * Copyright 2015 Hermann Lehner
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@SuppressWarnings("unused")
public class Main {

    public static final String PLUGIN_NAME = "Saitek-FIP4j";

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private DirectOutput directOutput = null;

    private AtomicInteger numberOfDemosRunning = new AtomicInteger(0);

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        try {
            LOGGER.info("Starting {}.", PLUGIN_NAME);
            directOutput = DirectOutputBuilder
                    .createAndLoadDLL()
                    .eventBus(new AsyncEventBus(new ForkJoinPool()))
                    .build();
            directOutput.setup(PLUGIN_NAME);
            Collection<Device> devices = directOutput.getDevices();
            directOutput.registerSubscriber(this);
            devices.forEach(this::setupDeviceForDemos);
            if (devices.isEmpty()) {
                int seconds = 0;
                LOGGER.info("Waiting 30 seconds for devices to be plugged in ...");
                while (directOutput.getDevices().isEmpty() && seconds++ < 30) {
                    Thread.sleep(1000);
                }
            }
            while (numberOfDemosRunning.get() > 0) {
                Thread.sleep(1000);
            }
        } catch (Throwable t) {
            logUnexpectedError(t);
        } finally {
            directOutput.cleanup();
        }
    }

    private void logUnexpectedError(Throwable t) {
        LOGGER.error("Awww, unexpected error.", t);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onDeviceChange(DeviceEvent event) {
        if (event.isConnected()) {
            setupDeviceForDemos(event.getDevice());
        }
    }

    private void setupDeviceForDemos(Device device) {
        numberOfDemosRunning.incrementAndGet();
        LOGGER.info("Running demo.");
        Page page = device.addPage();
        runDemos(page);
        numberOfDemosRunning.decrementAndGet();
    }

    private void runDemos(Page page) {
        new DeviceDemo(page).run();
    }

}
