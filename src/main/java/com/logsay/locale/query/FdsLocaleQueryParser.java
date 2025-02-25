package com.logsay.locale.query;

import java.util.Arrays;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.logdb.AbstractQueryCommandParser;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParserService;
import org.araqne.logdb.query.parser.ParseResult;
import org.araqne.logdb.query.parser.QueryTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsay.locale.LocaleManager;

@Component(name = "fdslocale-query-parser")
public class FdsLocaleQueryParser extends AbstractQueryCommandParser {
	private final Logger slog = LoggerFactory.getLogger(FdsLocaleQueryParser.class);
	
	@Requires
	private QueryParserService parserService;
	
	@Requires
	private LocaleManager localeManager;

	@Override
	public String getCommandName() {
		return "fdslocale";
	}
	
	@Validate
	public void start() {
		parserService.addCommandParser(this);
	}
	
	@Invalidate
	public void stop() {
		if (parserService != null)
			parserService.removeCommandParser(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		ParseResult r = QueryTokenizer.parseOptions(context, commandString, getCommandName().length(),
				Arrays.asList("ip"), getFunctionRegistry());
		Map<String, String> options = (Map<String, String>) r.value;
		String fn = options.get("ip");
		
		return new FdsLocaleQuery(localeManager, fn);
	}

}
