package com.example.ruttasktracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private DatabaseHelper dbHelper;
    private List<Task> taskList;
    private String selectedDueDate;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTaskStatusRunnable = new Runnable() {
        @Override
        public void run() {
            checkTaskStatus();
            handler.postDelayed(this, 60000); // Проверка каждую минуту
       }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateTaskStatusRunnable); // Начать обновление статуса задач, когда активен экран
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTaskStatusRunnable); // Остановить обновление, когда экран не активен
    }

    private void loadTasks() {
        taskList = dbHelper.getAllTasks();

        // Сортировка задач
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Collections.sort(taskList, (task1, task2) -> {
            try {
                Date date1 = dateFormat.parse(task1.getDueDate());
                Date date2 = dateFormat.parse(task2.getDueDate());
                boolean isTask1Overdue = date1.before(new Date()) || task1.isCompleted();
                boolean isTask2Overdue = date2.before(new Date()) || task2.isCompleted();

                // Задачи с актуальными сроками идут вверх
                if (isTask1Overdue && !isTask2Overdue) return 1;
                if (!isTask1Overdue && isTask2Overdue) return -1;

                // Сортировка по дате
                return date1.compareTo(date2);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });

        // Обновляем адаптер, передавая все необходимые слушатели
        taskAdapter = new TaskAdapter(this, taskList, this::onTaskStatusChanged,
                this::deleteTask, this::showEditTaskDialog);
        recyclerView.setAdapter(taskAdapter);
    }

    private void showAddTaskDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
        EditText edtDescription = dialogView.findViewById(R.id.edtDescription);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Добавить задачу")
                .setView(dialogView)
                .setPositiveButton("Выбрать дату", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                this,
                                (timeView, hourOfDay, minute) -> {
                                    Calendar now = Calendar.getInstance();
                                    if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                                            && calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                                            && (hourOfDay < now.get(Calendar.HOUR_OF_DAY) || (hourOfDay == now.get(Calendar.HOUR_OF_DAY) && minute < now.get(Calendar.MINUTE)))) {
                                        Toast.makeText(this, "Нельзя выбрать прошедшее время!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        calendar.set(Calendar.MINUTE, minute);

                                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                                        selectedDueDate = dateFormat.format(calendar.getTime());

                                        String title = edtTitle.getText().toString().trim();
                                        String description = edtDescription.getText().toString().trim();

                                        if (title.isEmpty() || description.isEmpty()) {
                                            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Task task = new Task(title, description, selectedDueDate, false);
                                            dbHelper.addTask(task);
                                            loadTasks();
                                            dialog.dismiss();
                                        }
                                    }
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                        );

                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });
    }

    private void onTaskStatusChanged(Task task, boolean isChecked) {
        task.setCompleted(isChecked);
        dbHelper.updateTask(task);
        loadTasks();
    }

    private void showEditTaskDialog(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
        EditText edtDescription = dialogView.findViewById(R.id.edtDescription);

        edtTitle.setText(task.getTitle());
        edtDescription.setText(task.getDescription());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Редактировать задачу")
                .setView(dialogView)
                .setPositiveButton("Выбрать дату", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                this,
                                (timeView, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);

                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                                    selectedDueDate = dateFormat.format(calendar.getTime());

                                    String title = edtTitle.getText().toString().trim();
                                    String description = edtDescription.getText().toString().trim();

                                    if (title.isEmpty() || description.isEmpty()) {
                                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                                    } else {
                                        task.setTitle(title);
                                        task.setDescription(description);
                                        task.setDueDate(selectedDueDate);
                                        dbHelper.updateTask(task);
                                        loadTasks();
                                        dialog.dismiss();
                                    }
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                        );
                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });
    }

    private void deleteTask(Task task) {
        dbHelper.deleteTask(task.getId()); // Передаем id задачи вместо объекта Task
        loadTasks();
    }

    // Метод для проверки задач на просроченность и изменения их статуса
    private void checkTaskStatus() {
        SimpleDateFormat dateFormatForCheck = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        for (Task task : taskList) {
            try {
                Date dueDate = dateFormatForCheck.parse(task.getDueDate());
                if (dueDate != null && dueDate.before(new Date()) && !task.isCompleted()) {
                    task.setCompleted(true);
                    dbHelper.updateTask(task); // Обновляем статус задачи в базе данных
                    loadTasks();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}