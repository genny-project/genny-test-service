package life.genny.tester.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import life.genny.qwandaq.entity.SearchEntity;

public class SearchGenerator {
	
	public static final String SEARCH_PREFIX = "SBE_TEST_";
	
	public static List<SearchEntity> generateRandomNameSearches(long count) {
		List<SearchEntity> searches = new ArrayList<SearchEntity>();
		for(long i = 0; i < count; i++) {
			searches.add(generateRandomNameSearch());
		}
		
		return searches;
	}
	
	public static SearchEntity testNameSearch(SearchEntity.StringFilter nameFilter, String nameQuery) {
		return new SearchEntity(SEARCH_PREFIX + "PERSONS", "Person Entities")
				.addFilter("PRI_NAME", nameFilter, nameQuery)
				.addSort("PRI_NAME", "Name", SearchEntity.Sort.ASC)
				.addColumn("PRI_NAME", "Name")
				.setPageIndex(0)
				.setPageSize(1000);
	}
	
	// TODO: Implement this stub method
	public static SearchEntity generateRandomSearch() {
		String code = "";
		String name = "";
		SearchEntity randSearch = new SearchEntity(code, name);
		
		return randSearch;
	}
	
	public static SearchEntity generateRandomNameSearch() {
		List<String> nameQueries = new ArrayList<>();
		nameQueries.add("Daniel");
		nameQueries.add("Lucas");
		nameQueries.add("Tom");
		nameQueries.add("Adam");
		nameQueries.add("Gizem");
		nameQueries.add("Rahul");
		nameQueries.add("Cyrus");
		nameQueries.add("Jasper");
		nameQueries.add("Ben");
		nameQueries.add("Aaron");
		nameQueries.add("Abay");
		nameQueries.add("Aashish");
		nameQueries.add("Abdul");
		
		// Pick a filter that isn't based on REGEXP or RLIKE
		SearchEntity.StringFilter[] filters = {
				SearchEntity.StringFilter.LIKE,
				SearchEntity.StringFilter.NOT_LIKE,
				SearchEntity.StringFilter.EQUAL,
				SearchEntity.StringFilter.NOT_EQUAL
		};
		
		SearchEntity.Sort[] sorts = SearchEntity.Sort.values();
		SearchEntity.Sort sort = pick(sorts);
		
		SearchEntity.StringFilter filter = pick(filters);
		
		String name = pick(nameQueries);
		
		if(SearchEntity.StringFilter.LIKE.equals(filter) 
		|| SearchEntity.StringFilter.NOT_LIKE.equals(filter))
			name += "%";
		
		return new SearchEntity(SEARCH_PREFIX + "PERSONS", "Person Entities")
				// Filters
				.addFilter("PRI_NAME", filter, name)
				.addFilter("PRI_IS_USER", true)
				// Sorts
				.addSort("PRI_NAME", "Name", sort)
				// Columns
				.addColumn("PRI_NAME", "Name")
				.setPageIndex(0)
				.setPageSize(1000);
	}
	
	private static <T> T pick(List<T> items) {
		Random random = new Random();
		int index = random.nextInt(items.size());
		
		return items.get(index);
	}
	
	private static <T> T pick(T[] items) {
		Random random = new Random();
		int index = random.nextInt(items.length);
		return items[index];
	}
}
