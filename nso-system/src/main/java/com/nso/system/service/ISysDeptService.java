package com.nso.system.service;

import com.nso.system.domain.SysDept;
import java.util.List;

public interface ISysDeptService {
    List<SysDept> list();
    SysDept create(SysDept dept);
}
