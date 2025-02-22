package com.ns.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private String taskID;
    private String taskName;
    private String membershipId;
    private List<SubTask> subTaskList;
}
