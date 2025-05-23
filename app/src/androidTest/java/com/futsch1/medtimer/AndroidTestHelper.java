package com.futsch1.medtimer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.icu.util.Calendar;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@SuppressWarnings("java:S2925")
public class AndroidTestHelper {
    static void setAllRemindersTo12AM() {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        setReminderTo12AM(0);
        setReminderTo12AM(1);
        setReminderTo12AM(2);
        setReminderTo12AM(3);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
    }

    public static void navigateTo(MainMenu mainMenu) {
        int[] menuIds = {R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment};
        clickOn(menuIds[mainMenu.ordinal()]);
        clickOn(menuIds[mainMenu.ordinal()]);
    }

    private static void setReminderTo12AM(int position) {
        clickListItem(R.id.medicineList, position);
        clickListItemChild(R.id.reminderList, 0, R.id.editReminderTime);

        setTime(0, 0, false);
        pressBack();
    }

    public static void setTime(int hour, int minute, boolean isDeltaTime) {
        if (!android.text.format.DateFormat.is24HourFormat(getInstrumentation().getTargetContext()) && !isDeltaTime) {
            clickOn(com.google.android.material.R.id.material_clock_period_am_button);
            if (hour == 12) {
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button);
            }
            if (hour > 12) {
                hour -= 12;
                clickOn(com.google.android.material.R.id.material_clock_period_pm_button);
            }
            if (hour == 0) {
                hour = 12;
            }
        }

        clickOn(com.google.android.material.R.id.material_timepicker_mode_button);
        clickOn(com.google.android.material.R.id.material_hour_text_input);
        onView(allOf(isDisplayed(), withClassName(is(TextInputEditText.class.getName())))).perform(replaceText(String.valueOf(hour)));
        clickOn(com.google.android.material.R.id.material_minute_text_input);
        onView(allOf(isDisplayed(), withClassName(is(TextInputEditText.class.getName())))).perform(replaceText(String.valueOf(minute)));
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button);
    }

    public static void editLatestEvent() {
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(0, new GeneralClickAction(
                Tap.SINGLE,
                GeneralLocation.TOP_RIGHT,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY)));
    }

    public static void createReminder(String amount, LocalTime time) {
        clickOn(R.id.addReminder);
        writeTo(R.id.editAmount, amount);

        if (time != null) {
            clickOn(R.id.editReminderTime);
            setTime(time.getHour(), time.getMinute(), false);
        }

        clickOn(R.id.createReminder);
    }

    public static void createIntervalReminder(String amount, int intervalMinutes) {
        clickOn(R.id.addReminder);
        writeTo(R.id.editAmount, amount);

        clickOn(R.id.intervalBased);
        clickOn(R.id.intervalMinutes);
        writeTo(R.id.editIntervalTime, String.valueOf(intervalMinutes));

        closeKeyboard();
        clickOn(R.id.createReminder);
    }

    public static void createMedicine(String name) {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        clickOn(R.id.addMedicine);
        writeTo(R.id.medicineName, name);

        clickDialogPositiveButton();
    }

    public static String dateToString(Date date) {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }

    public static LocalDateTime getNextNotificationTime() {
        Calendar rightNow = Calendar.getInstance();
        LocalDateTime dateTime = LocalDateTime.of(rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), 0);
        return dateTime.plusMinutes(2);
    }

    public enum MainMenu {OVERVIEW, MEDICINES, ANALYSIS}
}
