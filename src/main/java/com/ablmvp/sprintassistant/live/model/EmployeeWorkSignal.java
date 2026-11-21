package com.ablmvp.sprintassistant.live.model;

import java.util.List;

public record EmployeeWorkSignal(String assignee, int assignedIssueCount, int completedIssueCount,
                                 int remainingIssueCount, int inProgressIssueCount,
                                 double completedStoryPoints, double remainingStoryPoints,
                                 int blockedIssueCount, int overdueIssueCount, int workInProgressCount,
                                 String status, List<String> reasons) { }
