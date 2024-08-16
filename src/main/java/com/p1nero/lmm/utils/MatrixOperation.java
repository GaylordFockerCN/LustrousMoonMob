package com.p1nero.lmm.utils;
@FunctionalInterface
public interface MatrixOperation {
    OpenMatrix4f mul(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f dest);
}