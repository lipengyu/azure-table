package com.yammer.collections.azure.serialization.json;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Collection;

/**
* User: mrutkowski
* Date: 10/31/13
* Time: 9:20 PM
*/
@SuppressWarnings("UnusedDeclaration")
public class TestValuePojo {
    private final String name;
    private final Collection<Integer> numbers;


    public TestValuePojo(
            @JsonProperty("name") String name,
            @JsonProperty("numbers") Collection<Integer> numbers) {
        this.name = name;
        this.numbers = numbers;
    }

    public String getName() {
        return name;
    }

    public Collection<Integer> getNumbers() {
        return numbers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestValuePojo that = (TestValuePojo) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (numbers != null ? !numbers.equals(that.numbers) : that.numbers != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (numbers != null ? numbers.hashCode() : 0);
        return result;
    }
}
