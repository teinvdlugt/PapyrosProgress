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
