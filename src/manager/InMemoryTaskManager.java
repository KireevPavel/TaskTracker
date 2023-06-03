package manager;

import status.Status;
import tasks.Epic;
import tasks.Subtask;
import tasks.Task;

import java.time.Instant;
import java.util.*;


public class InMemoryTaskManager implements TaskManager {
    private int id = 0;

    protected Map<Integer, Task> tasks = new HashMap<>();
    protected Map<Integer, Subtask> subtasks = new HashMap<>();
    protected Map<Integer, Epic> epics = new HashMap<>();
    protected HistoryManager historyManager;
    private Compare compare = new Compare();
    private final Set<Task> prioritizedTasks = new TreeSet<>(compare);

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public int generateId() {
        return ++id;
    }

    @Override
    public Task createTask(Task task) {
        if (task == null) return null;
        int newTaskId = generateId();
        task.setId(newTaskId);
        addNewPrioritizedTask(task);
        tasks.put(newTaskId, task);
        return task;
    }

    @Override
    public Epic createEpic(Epic epic) {
        if (epic == null) return null;
        int newEpicId = generateId();
        epic.setId(newEpicId);
        epics.put(newEpicId, epic);
        return epic;
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        if (subtask == null) return null;
        int newSubtaskId = generateId();
        subtask.setId(newSubtaskId);
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null && !epic.getSubtaskIds().contains(newSubtaskId)) {
            addNewPrioritizedTask(subtask);
            subtasks.put(newSubtaskId, subtask);
            epic.setSubtaskIds(newSubtaskId);
            updateStatusEpic(epic);
            updateTimeEpic(epic);
            return subtask;
        } else {
            System.out.println("Epic not found");
            return null;
        }
    }

    @Override
    public void deleteTaskById(int id) {
        if (tasks.containsKey(id)) {
            prioritizedTasks.removeIf(task -> task.getId() == id);
            tasks.remove(id);
            historyManager.remove(id);
        } else {
            System.out.println("Task not found");
        }
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            epic.getSubtaskIds().forEach(subtaskId -> {
                prioritizedTasks.removeIf(task -> Objects.equals(task.getId(), subtaskId));
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            });
            epics.remove(id);
            historyManager.remove(id);
        } else {
            System.out.println("Epic not found");
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            epic.getSubtaskIds().remove((Integer) subtask.getId());
            updateStatusEpic(epic);
            updateTimeEpic(epic);
            prioritizedTasks.remove(subtask);
            subtasks.remove(id);
            historyManager.remove(id);
        } else {
            System.out.println("Subtask not found");
        }
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
        prioritizedTasks.clear();
    }

    @Override
    public void deleteAllEpics() {
        subtasks.clear();
        epics.clear();
    }

    @Override
    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            for (int subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.get(subtaskId);
                prioritizedTasks.remove(subtask);
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            epic.getSubtaskIds().clear();
        }
    }

    @Override
    public void deleteAllSubtasksByEpic(Epic epic) {
        if (epic != null) {
            for (int subtaskId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.get(subtaskId);
                prioritizedTasks.remove(subtask);
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
            epic.getSubtaskIds().clear();
        }
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public List<Task> getAllTasks() {
        if (tasks.size() == 0) {
            System.out.println("Task list is empty");
            return Collections.emptyList();
        }
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        if (epics.size() == 0) {
            System.out.println("Epic list is empty");
            return Collections.emptyList();
        }
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        if (subtasks.size() == 0) {
            System.out.println("Subtasks list is empty");
            return Collections.emptyList();
        }
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public List<Subtask> getAllSubtasksByEpicId(int id) {
        if (epics.containsKey(id)) {
            List<Subtask> subtasksNew = new ArrayList<>();
            Epic epic = epics.get(id);
            for (int i = 0; i < epic.getSubtaskIds().size(); i++) {
                subtasksNew.add(subtasks.get(epic.getSubtaskIds().get(i)));
            }
            return subtasksNew;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void updateTask(Task task) {
        if (task != null && tasks.containsKey(task.getId())) {
            addNewPrioritizedTask(task);
            tasks.put(task.getId(), task);
        } else {
            System.out.println("Task not found");
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic != null && epics.containsKey(epic.getId())) {
            epics.put(epic.getId(), epic);
            updateStatusEpic(epic);
            updateTimeEpic(epic);
        } else {
            System.out.println("Epic not found");
        }
    }

    @Override
    public void updateStatusEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            if (epic.getSubtaskIds().size() == 0) {
                epic.setStatus(Status.NEW);
            } else {
                List<Subtask> subtasksNew = new ArrayList<>();
                int countDone = 0;
                int countNew = 0;

                for (int i = 0; i < epic.getSubtaskIds().size(); i++) {
                    subtasksNew.add(subtasks.get(epic.getSubtaskIds().get(i)));
                }

                for (Subtask subtask : subtasksNew) {
                    if (subtask.getStatus() == Status.DONE) {
                        countDone++;
                    }
                    if (subtask.getStatus() == Status.NEW) {
                        countNew++;
                    }
                    if (subtask.getStatus() == Status.IN_PROGRESS) {
                        epic.setStatus(Status.IN_PROGRESS);
                        return;
                    }
                }

                if (countDone == epic.getSubtaskIds().size()) {
                    epic.setStatus(Status.DONE);
                } else if (countNew == epic.getSubtaskIds().size()) {
                    epic.setStatus(Status.NEW);
                } else {
                    epic.setStatus(Status.IN_PROGRESS);
                }
            }
        } else {
            System.out.println("Epic not found");
        }
    }

    public void updateTimeEpic(Epic epic) {
        List<Subtask> subtasks = getAllSubtasksByEpicId(epic.getId());
        Instant startTime = Instant.MAX;
        Instant endTime = Instant.MIN;

        for (Subtask subtask : subtasks) {
            if(subtask.getStartTime()!=null) {
                if (subtask.getStartTime().isBefore(startTime)) startTime = subtask.getStartTime();
            }
            if(subtask.getEndTime()!=null) {
                if (subtask.getEndTime().isAfter(endTime)) endTime = subtask.getEndTime();
            }
        }
        if(startTime!=Instant.MAX) {
            epic.setStartTime(startTime);
        }
        if(endTime!=Instant.MIN) {
            epic.setEndTime(endTime);
        }
        if(endTime!=Instant.MIN && startTime!=Instant.MAX) {
            long duration = (endTime.toEpochMilli() - startTime.toEpochMilli());
            epic.setDuration(duration);
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask != null && subtasks.containsKey(subtask.getId())) {
            addNewPrioritizedTask(subtask);
            subtasks.put(subtask.getId(), subtask);
            Epic epic = epics.get(subtask.getEpicId());
            updateStatusEpic(epic);
            updateTimeEpic(epic);
        } else {
            System.out.println("Subtask not found");
        }
    }

    @Override
    public void printTasks() {
        if (tasks.size() == 0) {
            System.out.println("Task list is empty");
            return;
        }
        for (Task task : tasks.values()) {
            System.out.println("Task{" +
                    "description='" + task.getDescription() + '\'' +
                    ", id=" + task.getId() +
                    ", name='" + task.getName() + '\'' +
                    ", status=" + task.getStatus() +
                    '}');
        }
    }

    @Override
    public void printEpics() {
        if (epics.size() == 0) {
            System.out.println("Epic list is empty");
            return;
        }
        for (Epic epic : epics.values()) {
            System.out.println("Epic{" +
                    "subtasksIds=" + epic.getSubtaskIds() +
                    ", description='" + epic.getDescription() + '\'' +
                    ", id=" + epic.getId() +
                    ", name='" + epic.getName() + '\'' +
                    ", status=" + epic.getStatus() +
                    '}');
        }
    }

    @Override
    public void printSubtasks() {
        if (subtasks.size() == 0) {
            System.out.println("Subtask list is empty");
            return;
        }
        for (Subtask subtask : subtasks.values()) {
            System.out.println("Subtask{" +
                    "epicId=" + subtask.getEpicId() +
                    ", description='" + subtask.getDescription() + '\'' +
                    ", id=" + subtask.getId() +
                    ", name='" + subtask.getName() + '\'' +
                    ", status=" + subtask.getStatus() +
                    '}');
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    public void addToHistory(int id) {
        if (epics.containsKey(id)) {
            historyManager.add(epics.get(id));
        } else if (subtasks.containsKey(id)) {
            historyManager.add(subtasks.get(id));
        } else if (tasks.containsKey(id)) {
            historyManager.add(tasks.get(id));
        }
    }

    @Override
    public void remove(int id) {
        historyManager.remove(id);
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    private void addNewPrioritizedTask(Task task) {
        prioritizedTasks.add(task);
        sortTaskPriority();
    }

    public boolean checkTime(Task task) {
        List<Task> tasks = List.copyOf(prioritizedTasks);
        int sizeTimeNull = 0;
        if (tasks.size() > 0) {
            for (Task taskSave : tasks) {
                if (taskSave.getStartTime() != null && taskSave.getEndTime() != null) {
                    if (task.getStartTime().isBefore(taskSave.getStartTime())
                            && task.getEndTime().isBefore(taskSave.getStartTime())) {
                        return true;
                    } else if (task.getStartTime().isAfter(taskSave.getEndTime())
                            && task.getEndTime().isAfter(taskSave.getEndTime())) {
                        return true;
                    }
                } else {
                    sizeTimeNull++;
                }

            }
            return sizeTimeNull == tasks.size();
        } else {
            return true;
        }
    }

    private void sortTaskPriority() {
        List<Task> tasks = getPrioritizedTasks();

        for (int i = 1; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            boolean taskHasIntersections = notIntersections(task);
            if(!taskHasIntersections){
                prioritizedTasks.remove(task);
            }
        }
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return prioritizedTasks.stream().toList();
    }

    @Override
    public String toString() {
        return "Manager{" +
                "tasks=" + tasks +
                ", subtasks=" + subtasks +
                ", epics=" + epics +
                ", historyManager=" + historyManager.getHistory() +
                '}';
    }
    private boolean notIntersections(Task task1) {
        if (task1 == null) {
            return false;
        } else if (task1.getStartTime() == null) {
            return true;
        }
        long startTimeTask = task1.getStartTime().toEpochMilli();
        if (task1.getEndTime() == null) {
            for (Task task : prioritizedTasks) {
                long startTime = task.getStartTime().toEpochMilli();
                if (task.getEndTime() == null) {
                    continue;
                }
                long end = task.getEndTime().toEpochMilli();
                if (startTimeTask >= startTime && startTimeTask <= end || startTimeTask == end) {
                    return false;
                }
            }
            return true;
        } else {
            long endTask = task1.getEndTime().toEpochMilli();
            for (Task task : prioritizedTasks) {
                long startTime = task.getStartTime().toEpochMilli();
                if (task.getEndTime() == null) {
                    if (startTime <= endTask && startTime >= startTimeTask || endTask == startTime) {
                        return false;
                    }
                    continue;
                }
                long end = task.getEndTime().toEpochMilli();
                if ((startTimeTask >= startTime && startTimeTask <= end) || (endTask >= startTime && endTask <= end)) {
                    return false;
                }
            }
        }
        return true;
    }
}

    class Compare implements Comparator<Task>{

        @Override
        public int compare(Task o1, Task o2) {
            if (o1.getStartTime() == null){
                return -1;
            } else if (o2.getStartTime() == null){
                return 1;
            }
            return (int)(o1.getStartTime().toEpochMilli() - o2.getStartTime().toEpochMilli());
        }
    }
