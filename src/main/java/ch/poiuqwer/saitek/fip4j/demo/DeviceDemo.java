package ch.poiuqwer.saitek.fip4j.demo;

import ch.poiuqwer.saitek.fip4j.Button;
import ch.poiuqwer.saitek.fip4j.*;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static ch.poiuqwer.saitek.fip4j.Button.*;
import static ch.poiuqwer.saitek.fip4j.KnobState.TURNED_CLOCKWISE;
import static ch.poiuqwer.saitek.fip4j.LedState.OFF;
import static ch.poiuqwer.saitek.fip4j.LedState.ON;
import static java.awt.Color.*;
import static java.awt.Font.*;

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
public class DeviceDemo {
    private static Logger LOGGER = LoggerFactory.getLogger(DeviceDemo.class);

    private static final Color TEXT_COLOR = new Color(191, 191, 191);
    private static final Color LIGHT_TEXT_COLOR = new Color(210, 210, 210);
    private static final Color VERY_DARK_GRAY = new Color(16, 16, 16);
    private static final int[] buttonYCoordinate = new int[]{3, 47, 90, 134, 177, 221};
    private static final int[] knobXCoordinate = new int[]{80, 217};

    private volatile boolean waitForKey;
    private volatile boolean blinkInProgress;

    private final Set<Button> toggleButtons = new HashSet<>();
    private final Page page;
    private final Device device;
    private final BufferedImage imageBuffer;
    private final Graphics g;

    private boolean exit = false;
    private boolean interactiveDemoRunning = false;


    public DeviceDemo(Page page) {
        this.page = page;
        this.device = page.getDevice();
        DirectOutput directOutput = page.getDirectOutput();
        directOutput.registerSubscriber(this);
        imageBuffer = DisplayBuffer.getSuitableBufferedImage();
        g = imageBuffer.getGraphics();
    }

    public void run() {
        setup();
        displayTitleText();
        drawColorBands();
        measureFrameRateDemo();
        interactiveButtonsDemo();
    }

    private void interactiveButtonsDemo() {
        if (exit) {
            return;
        }
        interactiveDemoRunning = true;
        drawButton(1, false);
        drawButton(2, false);
        drawButton(3, false);
        drawButton(4, false);
        drawButton(5, false);
        drawKnob(Knob.LEFT, null);
        drawKnob(Knob.RIGHT, null);
        drawDialog();
        waitForKey = true;
        while (waitForKey) {
            waitForKey = false;
            for (int i = 0; i < 100; i++) {
                sleep(100);
                if (exit) {
                    return;
                }
            }
        }
    }

    private void drawDialog() {
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(0, 0, 0, i * 30));
            g.fillRect(46, 0, 273, 206);
            g.setColor(new Color(90, 90, 90, i * 30));
            g.fillRect(100, 58, 175, 80);
            page.setImage(imageBuffer);
        }
        g.setColor(GRAY);
        g.drawRect(100, 58, 175, 80);
        g.setColor(LIGHT_TEXT_COLOR);
        Font font = new Font(DIALOG, PLAIN, 14);
        g.setFont(font);
        g.drawString("Try the buttons on the left", 104, 75);
        g.drawString("and the knobs below.", 104, 90);
        g.drawString("The demo stops after ten", 104, 115);
        g.drawString("seconds inactivity.", 104, 130);
        page.setImage(imageBuffer);
    }

    private void measureFrameRateDemo() {
        sleep(1000);
        boolean on = true;
        Font font = new Font(MONOSPACED, PLAIN, 14);
        g.setFont(font);
        g.setColor(TEXT_COLOR);
        g.drawString("Testing frame rate ...", 50, 157);

        page.setImage(imageBuffer);

        g.drawRect(68, 173, 228, 9);
        long start = System.nanoTime();
        int frames = 0;
        for (int i = 0; i < 225; i++) {
            if (exit) {
                return;
            }
            g.setColor(GRAY);
            g.drawLine(70 + i, 175, 70 + i, 180);
            if (i % 3 == 0) {
                page.setImage(imageBuffer);
                frames++;
                float duration = (System.nanoTime() - start) / 1000000000;
                float frameRate = frames / duration;
                g.setColor(BLACK);
                g.fillRect(240, 145, 100, 15);
                g.setColor(TEXT_COLOR);
                if (Float.isFinite(frameRate)) {
                    g.drawString(String.format("%5.2f fps", frameRate), 240, 157);
                }

            }
            if (i % 5 == 0) {
                page.setLed(UP, on ? ON : OFF);
                page.setLed(DOWN, on ? OFF : ON);
                on = !on;
            }
        }
        page.setImage(imageBuffer);
        page.setLed(UP, OFF);
        page.setLed(DOWN, OFF);
    }

    private void drawColorBands() {
        for (int i = 0; i < 256; i++) {
            g.setColor(new Color(i, 0, 0));
            g.drawLine(60 + i, 57, 60 + i, 77);
            g.setColor(new Color(0, i, 0));
            g.drawLine(60 + i, 85, 60 + i, 105);
            g.setColor(new Color(0, 0, i));
            g.drawLine(60 + i, 113, 60 + i, 133);
        }
        page.setImage(imageBuffer);
    }

    private void displayTitleText() {
        g.setColor(TEXT_COLOR);
        Font font = new Font(MONOSPACED, BOLD, 16);
        g.setFont(font);
        g.drawString("Demo Screen by Fip4j-Core", 55, 20);
        font = new Font(MONOSPACED, PLAIN, 12);
        g.setFont(font);
        g.drawString("Serial number: " + device.getSerialNumber(), 55, 39);
    }

    private void setup() {
        drawLayout();
        setupExitButton();
    }

    private void drawLayout() {
        g.setColor(VERY_DARK_GRAY);
        g.fillRect(0, 0, 45, 240);
        g.fillRect(46, 0, 273, 49);
        g.fillRect(46, 207, 273, 33);
        g.setColor(DARK_GRAY);
        g.drawLine(0, 33, 45, 33);
        g.drawLine(0, 77, 45, 77);
        g.drawLine(0, 120, 45, 120);
        g.drawLine(0, 164, 45, 164);
        g.drawLine(0, 206, 45, 206);
        g.drawLine(45, 0, 45, 240);
        g.drawLine(45, 50, 320, 50);
        g.drawLine(45, 140, 320, 140);
        g.drawLine(45, 206, 320, 206);
        g.drawLine(182, 206, 182, 240);
    }

    private void setupExitButton() {
        drawExitButton();
        page.setLed(S6, ON);
    }

    private void drawExitButton() {
        Font font = new Font(MONOSPACED, BOLD, 14);
        g.setFont(font);
        g.setColor(TEXT_COLOR);
        g.drawRoundRect(2, buttonYCoordinate[5], 40, 15, 3, 3);
        g.drawString("EXIT", 6, buttonYCoordinate[5] + 12);
    }

    private void drawButton(int i, boolean pressed) {
        Font font = new Font(MONOSPACED, BOLD, 14);
        g.setFont(font);
        g.setColor(pressed ? RED : DARK_GRAY);
        g.drawRoundRect(6, buttonYCoordinate[i - 1], 30, 15, 3, 3);
        g.drawString("S" + i + ">", 10, buttonYCoordinate[i - 1] + 12);
    }

    private void drawKnob(Knob knob, Button button) {
        Font font = new Font(MONOSPACED, BOLD, 14);
        g.setFont(font);
        int i = knob == Knob.LEFT ? 0 : 1;
        g.setColor(DARK_GRAY);
        g.drawRoundRect(knobXCoordinate[i], 217, 60, 15, 3, 3);
        g.drawLine(knobXCoordinate[i] + 30, 217, knobXCoordinate[i] + 30, 232);
        g.drawString("\u21b6", knobXCoordinate[i] + 8, 229);
        g.drawString("\u21b7", knobXCoordinate[i] + 38, 229);
        if (button == UP) {
            g.setColor(RED);
            g.drawRoundRect(knobXCoordinate[i], 217, 30, 15, 3, 3);
            g.drawString("\u21b6", knobXCoordinate[i] + 8, 229);
        }
        if (button == DOWN) {
            g.setColor(RED);
            g.drawRoundRect(knobXCoordinate[i] + 30, 217, 30, 15, 3, 3);
            g.drawString("\u21b7", knobXCoordinate[i] + 38, 229);
        }
    }

    private void blink(Knob knob, Button button) {
        if (!blinkInProgress) {
            synchronized (this) {
                if (!blinkInProgress) {
                    blinkInProgress = true;
                    drawKnob(knob, button);
                    drawKnob(knob == Knob.LEFT ? Knob.RIGHT : Knob.LEFT, null);
                    page.setImage(imageBuffer);
                    page.setLed(button, ON);
                    sleep(100);
                    drawKnob(Knob.LEFT, null);
                    drawKnob(Knob.RIGHT, null);
                    page.setImage(imageBuffer);
                    page.setLed(button, OFF);
                    blinkInProgress = false;
                }
            }
        }
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onPageChange(PageEvent event) {
        if (event.state == PageState.ACTIVE) {
            page.setImage(imageBuffer);
            for (Button button : toggleButtons) {
                page.setLed(button, ON);
            }
            page.setLed(S6, ON);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onButtonChange(ButtonEvent event) {
        if (event.state == ButtonState.PRESSED) {
            buttonPressed(event.button);
        } else {
            buttonReleased(event.button);
        }
    }

    private void buttonPressed(Button button) {
        LOGGER.info("Button pressed: {}", button);
        if (button == S6) {
            exit = true;
        } else if (interactiveDemoRunning) {
            if (toggleButtons.contains(button)) {
                drawButton(button.led, false);
                page.setImage(imageBuffer);
                page.setLed(button, OFF);
                toggleButtons.remove(button);
            } else {
                drawButton(button.led, true);
                page.setImage(imageBuffer);
                page.setLed(button, ON);
                toggleButtons.add(button);
            }
            page.setLed(UP, ON);
            page.setLed(DOWN, ON);
            waitForKey = true;
        }
    }

    private void buttonReleased(Button button) {
        LOGGER.info("Button released: {}", button);
        if (button != S6) {
            page.setLed(UP, OFF);
            page.setLed(DOWN, OFF);
            waitForKey = true;
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onKnobChange(KnobEvent event) {
        if (interactiveDemoRunning) {
            LOGGER.info("Knob turned {}: {}", event.state, event.knob);
            blink(event.knob, event.state == TURNED_CLOCKWISE ? DOWN : UP);
            waitForKey = true;
        }
    }

}
