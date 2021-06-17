package com.castsoftware.aip.console.tools.core.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonDto<T> {

    @NonNull
    T data;

    public static <T> JsonDto<T> of(T data) {
        return new JsonDto<>(data);
    }
}
