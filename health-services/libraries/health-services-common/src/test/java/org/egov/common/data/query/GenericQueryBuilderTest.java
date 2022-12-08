package org.egov.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.common.data.query.annotations.Table;
import org.egov.common.data.query.annotations.UpdateBy;
import org.egov.common.data.query.builder.SelectQueryBuilder;
import org.egov.common.data.query.builder.UpdateQueryBuilder;
import org.egov.common.data.query.exception.QueryBuilderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenericQueryBuilderTest {

    @Test
    @DisplayName("should build a select query based on data object and its primitive properties")
    void shouldBuildSelectQueryBasedOnDataObjectAndItsPrimitiveProperties() throws QueryBuilderException {
        DummyData data = DummyData.builder()
                .dummyString("some-string")
                .dummyInt(1)
                .build();
        String expectedQuery = "SELECT * FROM dummyData WHERE " +
                "dummyString=:dummyString AND dummyInt=:dummyInt";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("should not use primitive data types while building selecting query")
    void shouldNotUsePrimitiveDataTypesWhileBuildingSelectingQuery() throws QueryBuilderException {
        DummyData data = DummyData.builder()
                .dummyString("some-string")
                .dummyInt(1)
                .dummyPrimitiveBoolean(false)
                .dummyPrimitiveDouble(12.23)
                .dummyPrimitiveInt(12)
                .dummyPrimitiveFloat(232.2f)
                .build();
        String expectedQuery = "SELECT * FROM dummyData WHERE " +
                "dummyString=:dummyString AND dummyInt=:dummyInt";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("should not use where clause when properties are set to null while building select query")
    void shouldNotUseWhereClauseWhenPropertiesAreSetToNullSelectQuery() throws QueryBuilderException {
        DummyData data = DummyData.builder()
                .build();
        String expectedQuery = "SELECT * FROM dummyData";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("should use properties from nested object to build a select query")
    void shouldUsePropertiesFromNestedObjectToBuildSelectQuery() throws QueryBuilderException {
        DummyData data = DummyData.builder()
                .dummyString("TEST123")
                .dummyAddress(DummyAddress.builder().addressString("123").build())
                .build();
        String expectedQuery = "SELECT * FROM dummyData WHERE dummyString=:dummyString AND addressString=:addressString";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("should use properties from nested object's nested object to build a select query")
    void shouldUsePropertiesFrom2LevelNestedObjectToBuildSelectQuery() throws QueryBuilderException {
        DummyData data = DummyData.builder()
                .dummyString("TEST123")
                .dummyAddress(DummyAddress
                        .builder()
                        .addressString("123")
                        .dummyAmount(DummyAmount.builder().amount(123.0).currency("INR").build()).build())
                .build();
        String expectedQuery = "SELECT * FROM dummyData WHERE dummyString=:dummyString AND addressString=:addressString AND currency=:currency AND amount=:amount";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("Should use @updateBy to set the where clause")
    void shouldUseUpdateByAnnotationToSetTheWhereClause() throws QueryBuilderException{
        DummyData data = DummyData.builder()
                .dummyString("some-string")
                .dummyInt(1)
                .dummyAddress(DummyAddress
                        .builder()
                        .addressString("123").build())
                .build();
        String expectedQuery = "UPDATE dummyData SET dummyString=:dummyString , dummyInt=:dummyInt , addressString=:addressString WHERE dummyID=:dummyID";
        UpdateQueryBuilder queryBuilder = new UpdateQueryBuilder();

        String actualQuery = queryBuilder.build(data);

        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    @DisplayName("Should thorw QueryBuilderException for invalid object")
    void shouldThrowExceptionForInvalidObject(){
        DummyDataForException data = DummyDataForException.builder()
                .dummyString("some-string")
                .dummyID(1)
                .build();
        String expectedQuery = "UPDATE dummyData SET dummyString=:dummyString , dummyInt=:dummyInt , addressString=:addressString WHERE dummyID=:dummyID";
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder();

        assertThrows(QueryBuilderException.class, ()-> queryBuilder.build(data));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class DummyDataForException {
        private Integer dummyID;
        private String dummyString;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Table(name = "dummyData")
    static class DummyData {
        @UpdateBy
        private Integer dummyID;
        private String dummyString;
        private Integer dummyInt;
        private Boolean dummyBoolean;
        private Float dummyFloat;
        private Double dummyDouble;

        private int dummyPrimitiveInt;
        private boolean dummyPrimitiveBoolean;
        private float dummyPrimitiveFloat;
        private double dummyPrimitiveDouble;

        private DummyAddress dummyAddress;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class DummyAddress {
        @UpdateBy
        private String addressString;
        private DummyAmount dummyAmount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class DummyAmount {
        private String currency;
        private Double amount;
    }
}