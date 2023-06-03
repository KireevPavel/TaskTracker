package http;

import manager.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import status.Status;
import tasks.Epic;
import tasks.Subtask;
import tasks.Task;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HTTPTaskManagerTest<T extends TaskManagerTest<HTTPTaskManager>> {
    private KVServer server;
    private TaskManager manager;
    HistoryManager historyManager = Managers.getDefaultHistory();

    @BeforeEach
    public void createManager() {
        try {
            server = new KVServer();
            server.start();
            manager = Managers.getDefault(historyManager);
        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при создании менеджера");
        }
    }

    @AfterEach
    public void stopServer() {
        server.stop();
    }

    @Test
    public void shouldCorrectlySaveAndLoad() throws IOException, InterruptedException {

        Task task = new Task("Description", "Title", Status.NEW, Instant.now(), 1);
        manager.createTask(task);
        Epic epic = new Epic("Description", "Title", Status.NEW, Instant.now(), 2);
        manager.createEpic(epic);
        Subtask subtask = new Subtask("Description", "Title", Status.NEW, epic.getId()
                , Instant.now(), 3);

        manager.getTaskById(task.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(subtask.getId());

        HTTPTaskManager fileManager = Managers.getDefault(historyManager);
        fileManager.load();

        assertEquals(fileManager.getAllTasks(), manager.getAllTasks());
        assertEquals(fileManager.getAllEpics(), manager.getAllEpics());
        assertEquals(fileManager.getAllSubtasks(), manager.getAllSubtasks());
        assertEquals(fileManager.getPrioritizedTasks(), manager.getPrioritizedTasks());
        assertEquals(fileManager.getHistory(), manager.getHistory());
        assertEquals(fileManager.getTaskById(task.getId()), manager.getTaskById(task.getId()));
        assertEquals(fileManager.getEpicById(epic.getId()), manager.getEpicById(epic.getId()));
        assertEquals(fileManager.getSubtaskById(subtask.getId()), manager.getSubtaskById(subtask.getId()));
    }
}