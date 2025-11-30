package com.realtors.admin.service;

import java.util.List;
import java.util.Optional;

public interface BaseService<T, ID> {

    T create(T dto);

    Optional<T> findById(ID id);

    List<T> findAll();

    T update(ID id, T dto);

    boolean softDelete(ID id);
}
