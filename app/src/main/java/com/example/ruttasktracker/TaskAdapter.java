package com.example.ruttasktracker;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List; // Добавлен импорт
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final Context context;
    private final List<Task> taskList;  // Тип данных List<Task>
    private final OnTaskStatusChangeListener statusChangeListener;
    private final OnTaskDeleteListener deleteListener;
    private final OnTaskEditListener editListener;

    public TaskAdapter(Context context, List<Task> taskList,
                       OnTaskStatusChangeListener statusChangeListener,
                       OnTaskDeleteListener deleteListener,
                       OnTaskEditListener editListener) {
        this.context = context;
        this.taskList = taskList;
        this.statusChangeListener = statusChangeListener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        // Устанавливаем текстовые данные
        holder.txtTitle.setText(task.getTitle());
        holder.txtDescription.setText(task.getDescription());
        holder.txtDueDate.setText(task.getDueDate());

        // Проверяем, нужно ли зачеркнуть текст
        boolean isOverdue = isTaskOverdue(task.getDueDate());
        if (task.isCompleted() || isOverdue) {
            holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtDescription.setPaintFlags(holder.txtDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtDueDate.setPaintFlags(holder.txtDueDate.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtDescription.setPaintFlags(holder.txtDescription.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.txtDueDate.setPaintFlags(holder.txtDueDate.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // Устанавливаем состояние чекбокса
        holder.checkboxCompleted.setChecked(task.isCompleted());

        // Слушатель изменения состояния чекбокса
        holder.checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            statusChangeListener.onStatusChanged(task, isChecked);
        });

        // Слушатель удаления задачи
        holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> deleteListener.onDeleteTask(task));

        // Слушатель редактирования задачи
        holder.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> editListener.onEditTask(task));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private boolean isTaskOverdue(String dueDateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date dueDate = sdf.parse(dueDateString);
            return dueDate != null && dueDate.before(new Date());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDescription, txtDueDate;
        CheckBox checkboxCompleted;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Связываем элементы интерфейса
            txtTitle = itemView.findViewById(R.id.tvTitle);
            txtDescription = itemView.findViewById(R.id.tvDescription);
            txtDueDate = itemView.findViewById(R.id.tvDueDate);
            checkboxCompleted = itemView.findViewById(R.id.cbCompleted);
        }
    }

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, boolean isChecked);
    }

    public interface OnTaskDeleteListener {
        void onDeleteTask(Task task);
    }

    public interface OnTaskEditListener {
        void onEditTask(Task task);
    }
}