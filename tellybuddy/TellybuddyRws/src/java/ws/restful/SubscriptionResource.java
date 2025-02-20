package ws.restful;

import ejb.session.stateless.CustomerSessionBeanLocal;
import ejb.session.stateless.SubscriptionSessonBeanLocal;
import entity.Customer;
import entity.Subscription;
import entity.UsageDetail;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import util.exception.CreateNewSubscriptionException;
import util.exception.InvalidLoginCredentialException;
import util.exception.PhoneNumberInUseException;
import util.exception.PlanAlreadyDisabledException;
import util.exception.SubscriptionExistException;
import util.exception.SubscriptionNotFoundException;
import ws.datamodel.AllocateAddOnUnitsForCurrentMonthReq;
import ws.datamodel.AllocateAddOnUnitsForCurrentMonthRsp;
import ws.datamodel.AllocateQuizExtraUnitsReq;
import ws.datamodel.AllocateUnitsForNextMonthReq;
import ws.datamodel.AllocateUnitsForNextMonthRsp;
import ws.datamodel.CreateSubscriptionReq;
import ws.datamodel.CreateSubscriptionRsp;
import ws.datamodel.ErrorRsp;
import ws.datamodel.RetrieveActiveSubscriptionUnderCustomerRsp;
import ws.datamodel.RetrieveAllCustomerSubscriptionsRsp;
import ws.datamodel.RetrieveAllCustomerSubscriptionsWithBillsRsp;
import ws.datamodel.RetrieveSubscriptionRsp;
import ws.datamodel.RetrieveSubscriptionsUnderFamilyRsp;
import ws.datamodel.TerminateSubscriptionReq;

/**
 * REST Web Service
 *
 * @author tjle2
 */
@Path("Subscription")
public class SubscriptionResource {

    @Context
    private UriInfo context;

    private final SessionBeanLookup sessionBeanLookup;

    private final SubscriptionSessonBeanLocal subscriptionSessonBeanLocal;
    private final CustomerSessionBeanLocal customerSessionBeanLocal;

    public SubscriptionResource() {

        sessionBeanLookup = new SessionBeanLookup();

        subscriptionSessonBeanLocal = sessionBeanLookup.lookupSubscriptionSessonBeanLocal();
        customerSessionBeanLocal = sessionBeanLookup.lookupCustomerSessionBeanLocal();
    }

    /**
     * Retrieves representation of an instance of
     * ws.restful.SubscriptionResource
     *
     * @return an instance of java.lang.String
     */
    @Path("retrieveAllCustomerSubscriptions")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveAllCustomerSubscriptions(@QueryParam("username") String username, @QueryParam("password") String password) {
        try {
            Customer customer = customerSessionBeanLocal.customerLogin(username, password);
            System.out.println("********** SubscriptionResource.retrieveAllCustomerSubscriptions(): Customer " + customer.getUsername() + " login remotely via web service");

            List<Subscription> subscriptions = subscriptionSessonBeanLocal.retrieveAllSubscriptionUnderCustomer(customer);

            for (Subscription subscription : subscriptions) {
                subscription.setCustomer(null);
                for (UsageDetail usageDetail : subscription.getUsageDetails()) {
                    usageDetail.setSubscription(null);
                    usageDetail.setBill(null);
                }
                subscription.getPhoneNumber().setSubscription(null);
            }
            //instantiate the rsp class
            return Response.status(Response.Status.OK).entity(new RetrieveAllCustomerSubscriptionsRsp(subscriptions)).build();
        } catch (InvalidLoginCredentialException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorRsp).build();
        } catch (Exception ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
        }
    }

    @Path("retrieveSubscription/{subscriptionId}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveSubscriptionById(@QueryParam("username") String username, @QueryParam("password") String password, @PathParam("subscriptionId") Long subscriptionId) {
        try {
            Customer customer = customerSessionBeanLocal.customerLogin(username, password);
            System.out.println("********** SubscriptionResource.retrieveSubscription(): Customer " + customer.getUsername() + " login remotely via web service");

            Subscription subscription = subscriptionSessonBeanLocal.retrieveSubscriptionBySubscriptionId(subscriptionId);

            subscription.getCustomer().getQuizAttempts().clear();
            subscription.getCustomer().getSubscriptions().clear();
            subscription.getCustomer().getBills().clear();
            subscription.getCustomer().getTransactions().clear();
            subscription.getCustomer().setFamilyGroup(null);

            subscription.getCustomer().setPassword(null);
            subscription.getCustomer().setSalt(null);

            for (UsageDetail usageDetail : subscription.getUsageDetails()) {
                usageDetail.setSubscription(null);
                usageDetail.setBill(null);
            }

            subscription.getPhoneNumber().setSubscription(null);

            return Response.status(Response.Status.OK).entity(new RetrieveSubscriptionRsp(subscription)).build();
        } catch (InvalidLoginCredentialException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorRsp).build();
        } catch (SubscriptionNotFoundException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        } catch (Exception ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
        }
    }

    @Path("retrieveActive/{customerId}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response RetrieveActiveSubscriptionUnderCustomer(@QueryParam("username") String username, @QueryParam("password") String password, @QueryParam("customerId") Long customerId) {
        try {
            Customer customer = customerSessionBeanLocal.customerLogin(username, password);
            System.out.println("********** SubscriptionResource.retrieveAllCustomerSubscriptions(): Customer " + customer.getUsername() + " login remotely via web service");
            Customer customerToRetrieve = customerSessionBeanLocal.retrieveCustomerByCustomerId(customerId);
            List<Subscription> subscriptions = subscriptionSessonBeanLocal.retrieveAllActiveSubscriptionUnderCustomer(customerToRetrieve);

            for (Subscription subscription : subscriptions) {
                subscription.setCustomer(null);
                List<UsageDetail> uds = new ArrayList<>();
                uds = subscription.getUsageDetails();
                for (UsageDetail ud : uds) {
                    ud.setBill(null);
                    ud.setSubscription(null);
                }
                System.out.println("3");
                subscription.getPhoneNumber().setSubscription(null);
                System.out.println("4");
            }
            return Response.status(Response.Status.OK).entity(new RetrieveActiveSubscriptionUnderCustomerRsp(subscriptions)).build();
        } catch (InvalidLoginCredentialException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorRsp).build();
        } catch (Exception ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
        }
    }

    @Path("retrieveSubscriptionsUnderFamily/{familyGroupId}")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveSubscriptionsUnderFamily(@QueryParam("username") String username, @QueryParam("password") String password, @QueryParam("familyGroupId") Long familyGroupId) {
        try {
            Customer customer = customerSessionBeanLocal.customerLogin(username, password);
            System.out.println("********** SubscriptionResource.retrieveSubscriptionsUnderFamily(): Customer " + customer.getUsername() + " login remotely via web service");

            List<Subscription> subscriptions = subscriptionSessonBeanLocal.retrieveSubscriptionsOfFamilyByFamilyGroupId(familyGroupId);
            for (Subscription subscription : subscriptions) {

                subscription.setCustomer(null);
                subscription.getUsageDetails().clear();
                subscription.getPhoneNumber().setSubscription(null);
            }

            return Response.status(Response.Status.OK).entity(new RetrieveSubscriptionsUnderFamilyRsp(subscriptions)).build();
        } catch (InvalidLoginCredentialException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorRsp).build();
        } catch (Exception ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
        }
    }

    @Path("retrieveAllCustomerSubscriptionsWithBills")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveAllCustomerSubscriptionsWithBills(@QueryParam("username") String username, @QueryParam("password") String password) {
        try {
            Customer customer = customerSessionBeanLocal.customerLogin(username, password);
            System.out.println("********** SubscriptionResource.retrieveAllCustomerSubscriptionsWithBills(): Customer " + customer.getUsername() + " login remotely via web service");

            List<Subscription> subscriptions = subscriptionSessonBeanLocal.retrieveAllSubscriptionsWithBillsUnderCustomer(customer);

            for (Subscription subscription : subscriptions) {
                subscription.setCustomer(null);
                subscription.getUsageDetails().clear();
                subscription.getPhoneNumber().setSubscription(null);
            }

            return Response.status(Response.Status.OK).entity(new RetrieveAllCustomerSubscriptionsWithBillsRsp(subscriptions)).build();
        } catch (InvalidLoginCredentialException ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorRsp).build();
        } catch (Exception ex) {
            ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of SubscriptionResource
     *
     * @param content representation for the resource
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(CreateSubscriptionReq createSubscriptionReq) {

        if (createSubscriptionReq != null) {
            try {
                Customer customer = customerSessionBeanLocal.customerLogin(createSubscriptionReq.getUsername(), createSubscriptionReq.getPassword());
                System.out.println("********** SubscriptionResource.createSubscription(): Customer " + customer.getUsername() + " login remotely via web service");

                Subscription subscription = subscriptionSessonBeanLocal.createNewSubscription(createSubscriptionReq.getSubscription(), createSubscriptionReq.getPlanId(), createSubscriptionReq.getCustomerId(), createSubscriptionReq.getPhoneNumberId());
                CreateSubscriptionRsp createSubscriptionRsp = new CreateSubscriptionRsp(subscription.getSubscriptionId());

                return Response.status(Response.Status.OK).entity(createSubscriptionRsp).build();
            } catch (InvalidLoginCredentialException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.UNAUTHORIZED).entity(errorRsp).build();
            } catch (CreateNewSubscriptionException | PhoneNumberInUseException | SubscriptionExistException | PlanAlreadyDisabledException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            } catch (Exception ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
            }
        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid create new subscription request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    @Path("allocateUnitsForNextMonth")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response allocateUnitsForNextMonth(AllocateUnitsForNextMonthReq allocateUnitsForNextMonthReq) {

        if (allocateUnitsForNextMonthReq != null) {
            try {
                Customer customer = customerSessionBeanLocal.customerLogin(allocateUnitsForNextMonthReq.getUsername(), allocateUnitsForNextMonthReq.getPassword());
                System.out.println("********** SubscriptionResource.allocateUnitsForNextMonth(): Customer " + customer.getUsername() + " login remotely via web service");

                Subscription subscription = subscriptionSessonBeanLocal.allocateUnitsForNextMonth(allocateUnitsForNextMonthReq.getSubscription(), allocateUnitsForNextMonthReq.getDataUnits(), allocateUnitsForNextMonthReq.getSmsUnits(), allocateUnitsForNextMonthReq.getTalktimeUnits());
                AllocateUnitsForNextMonthRsp allocateUnitsForNextMonthRsp = new AllocateUnitsForNextMonthRsp(subscription.getSubscriptionId());
                return Response.status(Response.Status.OK).entity(allocateUnitsForNextMonthRsp).build();
            } catch (InvalidLoginCredentialException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.UNAUTHORIZED).entity(errorRsp).build();
            } catch (SubscriptionNotFoundException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            } catch (Exception ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
            }
        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid allocate units for next month request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    @Path("allocateAddOnUnitsForCurrentMonth")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response AllocateAddOnUnitsForCurrentMonth(AllocateAddOnUnitsForCurrentMonthReq allocateAddOnUnitsForCurrentMonthReq) {

        if (allocateAddOnUnitsForCurrentMonthReq != null) {
            try {
                Customer customer = customerSessionBeanLocal.customerLogin(allocateAddOnUnitsForCurrentMonthReq.getUsername(), allocateAddOnUnitsForCurrentMonthReq.getPassword());
                System.out.println("********** SubscriptionResource.allocateAddOnUnitsForCurrentMonth(): Customer " + customer.getUsername() + " login remotely via web service");

                Subscription subscription = subscriptionSessonBeanLocal.allocateAddOnUnitsForCurrentMonth(allocateAddOnUnitsForCurrentMonthReq.getSubscription(), allocateAddOnUnitsForCurrentMonthReq.getDataUnits(), allocateAddOnUnitsForCurrentMonthReq.getSmsUnits(), allocateAddOnUnitsForCurrentMonthReq.getTalktimeUnits());
                System.out.println("Customer: " + subscription.getSubscriptionId() + " purchased data add-ons" + subscription.getDataUnits().get("addOn"));
                System.out.println("Customer: " + subscription.getSubscriptionId() + " purchased Sms add-ons" + subscription.getSmsUnits().get("addOn"));
                System.out.println("Customer: " + subscription.getSubscriptionId() + " purchased talk time add-ons" + subscription.getTalkTimeUnits().get("addOn"));

                AllocateAddOnUnitsForCurrentMonthRsp allocateAddOnUnitsForCurrentMonthRsp = new AllocateAddOnUnitsForCurrentMonthRsp(subscription.getSubscriptionId());
                return Response.status(Response.Status.OK).entity(allocateAddOnUnitsForCurrentMonthRsp).build();
            } catch (InvalidLoginCredentialException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.UNAUTHORIZED).entity(errorRsp).build();
            } catch (SubscriptionNotFoundException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            } catch (Exception ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
            }
        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid add on units for current month request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    @Path("allocateQuizExtraUnits")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response allocateQuizExtraUnits(AllocateQuizExtraUnitsReq allocateQuizExtraUnitsReq) {

        if (allocateQuizExtraUnitsReq != null) {
            try {
                Customer customer = customerSessionBeanLocal.customerLogin(allocateQuizExtraUnitsReq.getUsername(), allocateQuizExtraUnitsReq.getPassword());
                System.out.println("********** SubscriptionResource.allocateQuizExtraUnits(): Customer " + customer.getUsername() + " login remotely via web service");

                Subscription subscription = subscriptionSessonBeanLocal.allocateQuizExtraUnits(allocateQuizExtraUnitsReq.getSubscription(), allocateQuizExtraUnitsReq.getDataUnits(), allocateQuizExtraUnitsReq.getSmsUnits(), allocateQuizExtraUnitsReq.getTalktimeUnits());

                System.out.println("added " + subscription.getDataUnits().get("quizExtraUnits") + " data units for " + subscription.getPhoneNumber().getPhoneNumber());
                System.out.println("added " + subscription.getSmsUnits().get("quizExtraUnits") + " SMS units for " + subscription.getPhoneNumber().getPhoneNumber());
                System.out.println("added " + subscription.getTalkTimeUnits().get("quizExtraUnits") + " talktime units for " + subscription.getPhoneNumber().getPhoneNumber());

                return Response.status(Response.Status.OK).build();
            } catch (InvalidLoginCredentialException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.UNAUTHORIZED).entity(errorRsp).build();
            } catch (SubscriptionNotFoundException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            } catch (Exception ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
            }
        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid allocate quiz extra units request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

    @Path("requestToTerminateSubscription")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestToTerminateSubscription(TerminateSubscriptionReq terminateSubscriptionReq) {

        if (terminateSubscriptionReq != null) {
            try {
                Customer customer = customerSessionBeanLocal.customerLogin(terminateSubscriptionReq.getUsername(), terminateSubscriptionReq.getPassword());
                System.out.println("********** SubscriptionResource.terminateSubscription(): Customer " + customer.getUsername() + " login remotely via web service");

                subscriptionSessonBeanLocal.requestToTerminateSubscription(terminateSubscriptionReq.getSubscription());

                return Response.status(Response.Status.OK).build();
            } catch (InvalidLoginCredentialException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.UNAUTHORIZED).entity(errorRsp).build();
            } catch (SubscriptionNotFoundException ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
            } catch (Exception ex) {
                ErrorRsp errorRsp = new ErrorRsp(ex.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorRsp).build();
            }
        } else {
            ErrorRsp errorRsp = new ErrorRsp("Invalid subscription termination request");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorRsp).build();
        }
    }

}
