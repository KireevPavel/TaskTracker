package http;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import manager.FileBackedTasksManager;
import manager.HistoryManager;
import manager.Managers;
import tasks.Epic;
import tasks.Subtask;
import tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HTTPTaskManager extends FileBackedTasksManager {

    final static String KEY_TASKS = "tasks";
    final static String KEY_SUBTASKS = "subtasks";
    final static String KEY_EPICS = "epics";
    final static String KEY_HISTORY = "history";
    final KVTaskClient client;
    private static final Gson gson =
            Managers.getGson();

    public HTTPTaskManager(HistoryManager historyManager, String path) throws IOException, InterruptedException {
        super(historyManager);
        client = new KVTaskClient(path);
    }

    public void load() {
        List<Task> tasks = new ArrayList<>();
        List<Epic> epics = new ArrayList<>();
        List<Subtask> subtasks = new ArrayList<>();
        ArrayList<Integer> history = new ArrayList<>();

        String jsonTasks = client.load("tasks");
        if(!jsonTasks.equals("null") && !jsonTasks.equals("")){
            tasks = gson.fromJson(jsonTasks,
                    new TypeToken<ArrayList<Task>>() {
                    }.getType());
        }

        String jsonEpic = client.load("epics");
        if(!jsonEpic.equals("null") && !jsonEpic.equals("")) {
            epics = gson.fromJson(jsonEpic,
                    new TypeToken<ArrayList<Epic>>() {
                    }.getType());
        }

        String jsonHistory = client.load("history");
        if(!history.equals("null") && !history.equals("")) {
            history = gson.fromJson(jsonHistory,
                    new TypeToken<ArrayList<Integer>>() {
                    }.getType());
        }
        for(Task task : tasks){
            createTask(task);
        }
       epics.forEach(this:: createEpic);
        subtasks.forEach(this:: createSubtask);
        for (Integer id: history){
            getTaskById(id);
            getEpicById(id);
            getSubtaskById(id);
        }
    }

    @Override
    public void save() {

        if (tasks.size() > 0) {
            client.put(KEY_TASKS, gson.toJson(tasks.values()));
        }
        if (subtasks.size() > 0) {
            client.put(KEY_SUBTASKS, gson.toJson(subtasks.values()));
        }
        if (epics.size() > 0) {
            client.put(KEY_EPICS, gson.toJson(epics.values()));
        }
           if(this.getHistory().size() > 0) {
               client.put(KEY_HISTORY, gson.toJson(this.getHistory()
                       .stream()
                       .map(Task::getId)
                       .collect(Collectors.toList())));
           }

    }
}