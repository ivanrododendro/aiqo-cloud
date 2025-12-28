package org.iro.aiqo.collector.logparser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Buffers {

    private Long sharedHit;
    private Long sharedRead;
    private Long sharedDirtied;
    private Long sharedWritten;
    private Long tempRead;
    private Long tempWritten;
}
