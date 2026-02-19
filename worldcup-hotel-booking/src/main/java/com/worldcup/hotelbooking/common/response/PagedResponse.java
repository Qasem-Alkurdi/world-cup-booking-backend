package com.worldcup.hotelbooking.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public static <T, E> PagedResponse<T> from(Page<E> pageObj, List<T> mappedContent) {
        PagedResponse<T> resp = new PagedResponse<>();
        resp.setContent(mappedContent);
        resp.setPage(pageObj.getNumber());
        resp.setSize(pageObj.getSize());
        resp.setTotalElements(pageObj.getTotalElements());
        resp.setTotalPages(pageObj.getTotalPages());
        resp.setLast(pageObj.isLast());
        return resp;
    }
}
