Feature: Bootstrap identity appliance
  In order to start defining my identity management architecture
  As an identity architect
  I want to create an identity appliance

  Scenario: Boostrap identity appliance from scratch
    Given I am on the modeler screen
    And my identity appliance name is 'Sample Identity Appliance'
    And my identity appliance description is 'This is my Sample Identity Appliance'
    And my identity appliance listens on host 'localhost'
    And my identity appliance listens on port 8081
    And my identity appliance listens on path 'sample'
    When I select advanced mode
    And I click on New button
    Then I should see 1 identity appliance element in the diagram canvas labeled as 'Sample Identity Appliance'

  Scenario: Boostrap identity appliance realizing a simple single sign-on setting
    Given I am on the modeler screen
    When I select Simple Single Sign-On mode
    And I click on New button
    Then I should see the identity appliance wizard

  Scenario: Create an identity appliance with an identity appliance under design
    Given I am on the modeler screen
    And I am modeling an identity appliance
    When I click on New button
    Then I should not be allowed

