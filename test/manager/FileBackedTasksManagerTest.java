package manager;

import manager.FileBackedTasksManager;
import manager.InMemoryTaskManager;
import manager.Managers;
import manager.TaskManagerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import status.Status;
import tasks.Epic;
import tasks.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;


import static org.junit.jupiter.api.Assertions.assertEquals;

class FileBackedTasksManagerTest extends TaskManagerTest<FileBackedTasksManager> {
    public static final Path path = Path.of("data.test.csv");
    File file = new File(String.valueOf(path));
    @BeforeEach
    public void beforeEach() {
        manager = new FileBackedTasksManager(Managers.getDefaultHistory(), file);
    }

    @AfterEach
    public void afterEach() {
        try {
            Files.delete(path);
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Test
    public void shouldCorrectlySaveAndLoad() {
        Task task = new Task("Description", "Title", Status.NEW, Instant.now(), 0);
        manager.createTask(task);
        Epic epic = new Epic("Description", "Title", Status.NEW, Instant.now(), 0);
        manager.createEpic(epic);
        FileBackedTasksManager fileManager = new FileBackedTasksManager(Managers.getDefaultHistory(), file);
        fileManager.loadFromFile();
        assertEquals(fileManager.getAllSubtasks(), manager.getAllSubtasks());
        assertEquals(fileManager.getAllEpics(), manager.getAllEpics());
        assertEquals(fileManager.getPrioritizedTasks(), manager.getPrioritizedTasks());
        assertEquals(fileManager.getHistory(), manager.getHistory());
        assertEquals(fileManager.getTaskById(task.getId()), manager.getTaskById(task.getId()));
        assertEquals(fileManager.getEpicById(epic.getId()), manager.getEpicById(epic.getId()));
    }

    @Test
    public void shouldSaveAndLoadEmptyTasksEpicsSubtasks() {
        FileBackedTasksManager fileManager = new FileBackedTasksManager(Managers.getDefaultHistory(), file);
        fileManager.save();
        fileManager.loadFromFile();
        assertEquals(Collections.EMPTY_LIST, manager.getAllTasks());
        assertEquals(Collections.EMPTY_LIST, manager.getAllEpics());
        assertEquals(Collections.EMPTY_LIST, manager.getAllSubtasks());
    }

    @Test
    public void shouldSaveAndLoadEmptyHistory() {
        FileBackedTasksManager fileManager = new FileBackedTasksManager(Managers.getDefaultHistory(), file);
        fileManager.save();
        fileManager.loadFromFile();
        assertEquals(Collections.EMPTY_LIST, manager.getHistory());
    }
}