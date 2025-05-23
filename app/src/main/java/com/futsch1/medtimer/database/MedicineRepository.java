package com.futsch1.medtimer.database;

import android.app.Application;
import android.util.Log;
import com.futsch1.medtimer.LogTags;

import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MedicineRepository {

    private final MedicineDao medicineDao;
    private final MedicineRoomDatabase database;
    @SuppressWarnings("java:S6204") // Stream.toList() not available in SDK version selected
    private final List<ReminderEvent.ReminderStatus> allStatusValues = Arrays.stream(new ReminderEvent.ReminderStatus[]{ReminderEvent.ReminderStatus.DELETED, ReminderEvent.ReminderStatus.RAISED, ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.TAKEN}).
            collect(Collectors.toList());
    @SuppressWarnings("java:S6204") // Stream.toList() not available in SDK version selected
    private final List<ReminderEvent.ReminderStatus> statusValuesWithoutDelete = Arrays.stream(new ReminderEvent.ReminderStatus[]{ReminderEvent.ReminderStatus.RAISED, ReminderEvent.ReminderStatus.SKIPPED, ReminderEvent.ReminderStatus.TAKEN}).
            collect(Collectors.toList());

    public MedicineRepository(Application application) {
        database = MedicineRoomDatabase.getDatabase(application);
        medicineDao = database.medicineDao();
    }

    public int getVersion() {
        return database.getVersion();
    }

    public LiveData<List<FullMedicine>> getLiveMedicines() {
        return medicineDao.getLiveMedicines();
    }

    public List<FullMedicine> getMedicines() {
        return medicineDao.getMedicines();
    }

    public Medicine getOnlyMedicine(int medicineId) {
        return medicineDao.getOnlyMedicine(medicineId);
    }

    public LiveData<FullMedicine> getLiveMedicine(int medicineId) {
        return medicineDao.getLiveMedicine(medicineId);
    }

    public FullMedicine getMedicine(int medicineId) {
        return medicineDao.getMedicine(medicineId);
    }

    public LiveData<List<Reminder>> getLiveReminders(int medicineId) {
        return medicineDao.getLiveReminders(medicineId);
    }

    public List<Reminder> getReminders(int medicineId) {
        return medicineDao.getReminders(medicineId);
    }

    public Reminder getReminder(int reminderId) {
        return medicineDao.getReminder(reminderId);
    }

    public LiveData<List<ReminderEvent>> getLiveReminderEvents(long timeStamp, boolean withDeleted) {
        return medicineDao.getLiveReminderEventsStartingFrom(timeStamp, withDeleted ? allStatusValues : statusValuesWithoutDelete);
    }

    public List<ReminderEvent> getAllReminderEventsWithoutDeleted() {
        return medicineDao.getLimitedReminderEvents(0L, statusValuesWithoutDelete);
    }

    public List<ReminderEvent> getLastDaysReminderEvents(int days) {
        return medicineDao.getLimitedReminderEvents(Instant.now().toEpochMilli() / 1000 - ((long) days * 24 * 60 * 60), allStatusValues);
    }

    public long insertMedicine(Medicine medicine) {
        Log.i(LogTags.REMINDER,"Check insert medicine");
        return internalInsert(medicine, medicineDao::insertMedicine);
    }

    private <T> long internalInsert(T insertType, Insert<T> f) {
        try {
            return MedicineRoomDatabase.databaseWriteExecutor.submit(() -> f.insert(insertType)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e1) {
            return -1;
        }
        return 0;
    }

    public void updateMedicine(Medicine medicine) {
        Log.i(LogTags.REMINDER,"Check update medicine");
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateMedicine(medicine));
    }

    public void deleteMedicine(int medicineId) {
        Log.i(LogTags.REMINDER,"Check delete medicine");
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> {
            medicineDao.deleteMedicineToTagForMedicine(medicineId);
            medicineDao.deleteMedicine(medicineDao.getOnlyMedicine(medicineId));
        });
    }

    public long insertReminder(Reminder reminder) {
        Log.i(LogTags.REMINDER,"Check insert reminder");
        return internalInsert(reminder, medicineDao::insertReminder);
    }

    public void updateReminder(Reminder reminder) {
        Log.i(LogTags.REMINDER,"Check update reminder");
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminder(reminder));
    }

    public void deleteReminder(int reminderId) {
        Log.i(LogTags.REMINDER,"Check delete reminder");
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteReminder(medicineDao.getReminder(reminderId)));
    }

    public long insertReminderEvent(ReminderEvent reminderEvent) {
        return internalInsert(reminderEvent, medicineDao::insertReminderEvent);
    }

    public @Nullable ReminderEvent getReminderEvent(int reminderEventId) {
        return medicineDao.getReminderEvent(reminderEventId);
    }

    public void updateReminderEvent(ReminderEvent reminderEvent) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.updateReminderEvent(reminderEvent));
    }

    public void deleteAll() {
        deleteReminders();
        deleteMedicines();
        deleteReminderEvents();
        deleteTags();
    }

    public void deleteReminders() {
        Log.i(LogTags.REMINDER,"Check delete reminders");
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminders);
    }

    public void deleteMedicines() {
        Log.i(LogTags.REMINDER,"Check delete medicines");
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteMedicines);
    }

    public void deleteReminderEvents() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteReminderEvents);
    }

    public void deleteTags() {
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteTags);
        MedicineRoomDatabase.databaseWriteExecutor.execute(medicineDao::deleteMedicineToTags);
    }

    public void deleteReminderEvent(int reminderEventId) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteReminderEvent(medicineDao.getReminderEvent(reminderEventId)));
    }

    public List<Reminder> getLinkedReminders(int reminderId) {
        return medicineDao.getLinkedReminders(reminderId);
    }

    @NotNull
    public LiveData<List<Tag>> getLiveTags() {
        return medicineDao.getLiveTags();
    }

    public long insertTag(@NotNull Tag tag) {
        Tag existingTag = getTagByName(tag.name);
        if (existingTag == null) {
            return internalInsert(tag, medicineDao::insertTag);
        } else {
            return existingTag.tagId;
        }
    }

    public Tag getTagByName(String name) {
        return medicineDao.getTagByName(name);
    }

    public void deleteTag(@NotNull Tag tag) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> {
            medicineDao.deleteMedicineToTagForTag(tag.tagId);
            medicineDao.deleteTag(tag);
        });
    }

    public void insertMedicineToTag(int medicineId, int tagId) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.insertMedicineToTag(new MedicineToTag(medicineId, tagId)));
    }

    public void deleteMedicineToTag(int medicineId, int tagId) {
        MedicineRoomDatabase.databaseWriteExecutor.execute(() -> medicineDao.deleteMedicineToTag(new MedicineToTag(medicineId, tagId)));
    }

    @NotNull
    public LiveData<List<MedicineToTag>> getLiveMedicineToTags() {
        return medicineDao.getLiveMedicineToTags();
    }

    public boolean hasTags() {
        return medicineDao.countTags() > 0;
    }

    interface Insert<T> {
        long insert(T item);
    }
}
