/*
 * Papyros Progress: An Android application showing the development progress of Papyros
 * Copyright (C) 2016  Tein van der Lugt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.teinproductions.tein.papyrosprogress;

/**
 * Model class for a milestone
 */
public class Milestone {
    private String title;
    private int openIssues, closedIssues;
    private String state;
    private long createdAt, updatedAt, dueOn, closedAt;
    private String githubUrl;

    public Milestone() {
    }

    public Milestone(String title, int openIssues, int closedIssues, String state,
                     long createdAt, long updatedAt, long dueOn, long closedAt, String githubUrl) {
        this.title = title;
        this.openIssues = openIssues;
        this.closedIssues = closedIssues;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.dueOn = dueOn;
        this.closedAt = closedAt;
        this.githubUrl = githubUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(int openIssues) {
        this.openIssues = openIssues;
    }

    public int getClosedIssues() {
        return closedIssues;
    }

    public void setClosedIssues(int closedIssues) {
        this.closedIssues = closedIssues;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public int getProgress() {
        return closedIssues * 100 / (openIssues + closedIssues);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getDueOn() {
        return dueOn;
    }

    public void setDueOn(long dueOn) {
        this.dueOn = dueOn;
    }

    public long getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(long closedAt) {
        this.closedAt = closedAt;
    }
}
