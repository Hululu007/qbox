package com.fongmi.chaquo;

/**
 * QBox：未编入 Chaquopy/Python 运行时（体积与构建成本考虑），
 * 这里提供占位实现，Python 类型站点会回退为 SpiderNull。
 */
public class Loader {

    public com.github.catvod.crawler.Spider spider(String api) throws Exception {
        throw new UnsupportedOperationException("Python runtime is not bundled in QBox");
    }
}
