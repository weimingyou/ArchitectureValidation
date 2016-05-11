package com.weimingyou.architecturevalidator;

public class ValidationException extends Exception {
	private static final long serialVersionUID = -1670505118308868523L;

	public ValidationException(String msg, Object... args) {
		super(String.format(msg, args));
	}
}
