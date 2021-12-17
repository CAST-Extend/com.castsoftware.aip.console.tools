package com.castsoftware.aip.console.tools.core.dto.export;

import com.castsoftware.aip.console.tools.core.dto.BaseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MavenRepositoryDto extends BaseDto {
    private String url;
    private String username;
    private String password;
}

