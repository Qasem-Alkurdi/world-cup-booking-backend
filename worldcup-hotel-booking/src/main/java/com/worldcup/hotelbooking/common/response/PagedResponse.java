package com.worldcup.hotelbooking.common.response;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

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