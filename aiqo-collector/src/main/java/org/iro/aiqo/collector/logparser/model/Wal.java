package org.iro.aiqo.collector.logparser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wal {

    private Long records;
    private Long fpi;
    private Long bytes;
}
