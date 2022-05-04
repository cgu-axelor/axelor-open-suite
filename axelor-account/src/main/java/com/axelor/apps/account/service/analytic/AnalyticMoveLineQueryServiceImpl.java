/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.service.analytic;

import com.axelor.apps.account.db.AnalyticAccount;
import com.axelor.apps.account.db.AnalyticAxis;
import com.axelor.apps.account.db.AnalyticMoveLine;
import com.axelor.apps.account.db.AnalyticMoveLineQuery;
import com.axelor.apps.account.db.AnalyticMoveLineQueryParameter;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.common.ObjectUtils;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AnalyticMoveLineQueryServiceImpl implements AnalyticMoveLineQueryService {

  AnalyticMoveLineService analyticMoveLineService;

  @Inject
  public AnalyticMoveLineQueryServiceImpl(AnalyticMoveLineService analyticMoveLineService) {
    this.analyticMoveLineService = analyticMoveLineService;
  }

  @Override
  public String getAnalyticMoveLineQuery(AnalyticMoveLineQuery analyticMoveLineQuery) {
    String query = "self.moveLine.move.company.id = " + analyticMoveLineQuery.getCompany().getId();

    if (Beans.get(AppBaseService.class).getAppBase().getEnableTradingNamesManagement()
        && ObjectUtils.notEmpty(analyticMoveLineQuery.getTradingName())) {
      query +=
          " AND self.moveLine.move.tradingName.id = "
              + analyticMoveLineQuery.getTradingName().getId();
    }

    query +=
        String.format(" AND self.date >= '%s'", analyticMoveLineQuery.getFromDate().toString());
    query += String.format(" AND self.date <= '%s'", analyticMoveLineQuery.getToDate().toString());

    query += " AND self.moveLine.move.statusSelect = ";

    Integer specificOriginSelect = analyticMoveLineQuery.getSpecificOriginSelect();
    int statusAccounted = MoveRepository.STATUS_ACCOUNTED;
    int statusDaybook = MoveRepository.STATUS_DAYBOOK;

    switch (specificOriginSelect) {
      case 1:
        query += statusAccounted + " AND self.invoiceLine IS null";
        break;
      case 2:
        query += statusAccounted + " AND self.invoiceLine IS NOT null";
        break;
      case 3:
        query +=
            statusAccounted + " AND self.invoiceLine IS NOT null AND self.moveLine IS NOT null";
        break;
      case 4:
        query += statusDaybook + " AND self.invoiceLine IS null";
        break;
      case 5:
        query += statusDaybook + " AND self.invoiceLine IS NOT null";
        break;
      case 6:
        query += statusDaybook + " AND self.invoiceLine IS NOT null AND self.moveLine IS NOT null";
        break;
      case 7:
        query += MoveRepository.STATUS_SIMULATED;
        break;
      default:
        break;
    }

    Integer searchOperatorSelect = analyticMoveLineQuery.getSearchOperatorSelect();
    query += " AND";
    List<AnalyticMoveLineQueryParameter> searchAnalyticMoveLineQueryParameterList =
        analyticMoveLineQuery.getSearchAnalyticMoveLineQueryParameterList();
    int size = searchAnalyticMoveLineQueryParameterList.size();
    int i = 0;
    for (AnalyticMoveLineQueryParameter parameter : searchAnalyticMoveLineQueryParameterList) {
      i++;
      switch (searchOperatorSelect) {
        case 0:
          query += " (self.analyticAxis.id = " + parameter.getAnalyticAxis().getId();
          if (ObjectUtils.notEmpty(parameter.getAnalyticAccountSet())) {
            query +=
                " AND self.analyticAxis.id IN ("
                    + parameter.getAnalyticAccountSet().stream()
                        .map(l -> l.getAnalyticAxis().getId().toString())
                        .collect(Collectors.joining(","))
                    + ")";
          }
          query += ")";
          if (size > i) {
            query += " OR";
          }
          break;
        case 1:
          query += " (self.analyticAxis.id = " + parameter.getAnalyticAxis().getId();
          if (ObjectUtils.notEmpty(parameter.getAnalyticAccountSet())) {
            query +=
                " AND self.analyticAxis.id IN ("
                    + parameter.getAnalyticAccountSet().stream()
                        .map(l -> l.getAnalyticAxis().getId().toString())
                        .collect(Collectors.joining(","))
                    + ")";
          }
          query += ")";
          if (size > i) {
            query += " AND";
          }
          break;
        default:
          break;
      }
    }
    return query;
  }

  @Override
  public Set<AnalyticMoveLine> analyticMoveLineReverses(
      AnalyticMoveLineQuery analyticMoveLineQuery, List<AnalyticMoveLine> analyticMoveLines) {

    Map<AnalyticAxis, AnalyticAccount> reverseRules =
        analyticMoveLineQuery.getReverseAnalyticMoveLineQueryParameterList().stream()
            .collect(
                Collectors.toMap(
                    AnalyticMoveLineQueryParameter::getAnalyticAxis,
                    AnalyticMoveLineQueryParameter::getAnalyticAccount));

    Set<AnalyticMoveLine> reverseAnalyticMoveLines = new HashSet<AnalyticMoveLine>();
    for (AnalyticAxis analyticAxis : reverseRules.keySet()) {
      AnalyticAccount analyticAccount = reverseRules.get(analyticAxis);
      List<AnalyticMoveLine> analyticMoveLinesToReverse =
          analyticMoveLines.stream()
              .filter(
                  analyticMoveLine ->
                      Objects.equals(analyticMoveLine.getAnalyticAxis(), analyticAxis))
              .collect(Collectors.toList());

      reverseAnalyticMoveLines.addAll(
          analyticMoveLineReverses(analyticAccount, analyticMoveLinesToReverse));
    }

    return reverseAnalyticMoveLines;
  }

  @Transactional
  protected Set<AnalyticMoveLine> analyticMoveLineReverses(
      AnalyticAccount analyticAccount, List<AnalyticMoveLine> analyticMoveLines) {

    return analyticMoveLines.stream()
        .map(
            analyticMoveLine ->
                analyticMoveLineService.reverseAndPersist(analyticMoveLine, analyticAccount))
        .collect(Collectors.toSet());
  }
}
