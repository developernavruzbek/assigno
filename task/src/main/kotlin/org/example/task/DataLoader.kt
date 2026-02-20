package org.example.task

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
/*
@Component
class DefaultTaskStatesInitializer(
    private val taskStateRepository: TaskStateRepository
) {
    @PostConstruct
    fun init() {
        val defaults = listOf(
            TaskState("Todo", "TODO"),
            TaskState("In Progress", "IN_PROGRESS"),
            TaskState("Done", "DONE")
        )

        defaults.forEach {
            if (!taskStateRepository.existsByCode(it.code)) {
                taskStateRepository.save(it)
            }
        }
    }
}


 */