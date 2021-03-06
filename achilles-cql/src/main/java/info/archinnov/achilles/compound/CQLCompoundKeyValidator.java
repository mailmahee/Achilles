/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.archinnov.achilles.compound;

import static info.archinnov.achilles.type.OrderingMode.ASCENDING;
import info.archinnov.achilles.type.OrderingMode;
import info.archinnov.achilles.validation.Validator;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CQLCompoundKeyValidator extends CompoundKeyValidator {
	private static final Logger log = LoggerFactory.getLogger(CQLCompoundKeyValidator.class);

	@Override
	public void validateComponentsForSliceQuery(List<Object> startComponents, List<Object> endComponents,
			OrderingMode ordering) {
		String startDescription = StringUtils.join(startComponents, ",");
		String endDescription = StringUtils.join(endComponents, ",");

		log.trace("Check compound keys {} / {}", startDescription, endDescription);

		int startIndex = getLastNonNullIndex(startComponents);
		int endIndex = getLastNonNullIndex(endComponents);

		// No more than 1 non-null component difference between clustering keys
		Validator.validateTrue(Math.abs(endIndex - startIndex) <= 1,
				"There should be no more than 1 component difference between clustering keys: [[%s],[%s]",
				startDescription, endDescription);

		if (startIndex < 0 || endIndex < 0) {
			return;
		} else {
			int equalComponentsIndex = Math.max(startIndex, endIndex) - 1;
			for (int i = 0; i <= equalComponentsIndex; i++) {
				Object startComp = startComponents.get(i);
				Object endComp = endComponents.get(i);

				int comparisonResult = comparator.compare(startComp, endComp);

				Validator.validateTrue(comparisonResult == 0, (i + 1)
						+ "th component for clustering keys should be equal: [[%s],[%s]", startDescription,
						endDescription);
			}

			if (startIndex == endIndex) {
				Object startComp = startComponents.get(startIndex);
				Object endComp = endComponents.get(endIndex);
				if (ASCENDING.equals(ordering)) {
					Validator
							.validateTrue(
									comparator.compare(startComp, endComp) <= 0,
									"For slice query with ascending order, start clustering last component should be 'lesser or equal' to end clustering last component: [[%s],[%s]",
									startDescription, endDescription);
				} else {
					Validator
							.validateTrue(
									comparator.compare(startComp, endComp) >= 0,
									"For slice query with descending order, start clustering last component should be 'greater or equal' to end clustering last component: [[%s],[%s]",
									startDescription, endDescription);
				}

			}
		}
	}

}
