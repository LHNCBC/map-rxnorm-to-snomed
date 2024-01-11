package gov.nih.nlm.mor.RxNorm;

import java.util.Objects;

public class Property {
	public String name;
	public String id;
	public String value;
	public String category;  //ATTRIBUTES or CODES or NAMES or SOURCES
	
	public Property() {
		
	}
	
	public Property(String n, String i, String v, String c) {
		this.name = n;
		this.id = i.replace(" ", "_");
		this.value = v;
		this.category = c;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String getCategory() {
		return this.category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
	public boolean isAttributes() {
		if(this.category.equalsIgnoreCase("ATTRIBUTES")) return true;
		return false;
	}
	
	public boolean isCodes() {
		if(this.category.equalsIgnoreCase("CODES")) return true;
		return false;
	}
	
	public boolean isNames() {
		if(this.category.equalsIgnoreCase("NAMES")) return true;
		return false;
	}
	
	public boolean isSources() {
		if(this.category.equalsIgnoreCase("SOURCES")) return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id, name, value, category);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		return Objects.equals(category, other.category) && Objects.equals(id, other.id) && Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}

}
