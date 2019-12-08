package it.usuratonkachi.telegranloader.telegranloader.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Builder
@Accessors(chain = true)
public class Response implements Serializable {

    private String message;

}
