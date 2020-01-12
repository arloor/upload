package com.arloor.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IpVo {
    String ip;
    String isoCode;
    String name;
    String nameZhCN;


    public String toString(){
        return String.format("You are from {ip=%s, code=%s, name=%s}!",ip,isoCode,nameZhCN);
    }
}
